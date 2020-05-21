package tothrosch.networking.rest.adapter.bitmex

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlinx.coroutines.channels.Channel
import org.apache.commons.codec.binary.Hex
import org.apache.http.HttpResponse
import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity
import tothrosch.engine.Keep
import tothrosch.engine.message.OrderRequest
import tothrosch.engine.message.Request
import tothrosch.engine.mode
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.Instrument
import tothrosch.instrument.Instruments
import tothrosch.instrument.Mode
import tothrosch.instrument.book.Book
import tothrosch.json.bitmex.BitmexOrderRequestSerializer
import tothrosch.networking.rest.RestClient
import tothrosch.util.HttpDeleteWithBody
import tothrosch.util.log
import tothrosch.util.time.TimedAction
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by ndrsh on 30.06.17.
 */

@Keep
object BitmexRestClient : RestClient(Exchange.BitmexExchange) {
	private val baseAddress: String = "https://www.bitmex.com"
	private val mac: Mac = Mac.getInstance("HmacSHA256")
	@Volatile var retryAt: Long = 0L
	@Volatile var remaining: Int = 60

	// note that this variable is not threadsafe, but fuck it, because we are not doing
	// so many orderchanges at the same time (the only time where this variable is accessed)
	@Volatile
	var lastPrintedRemaining = 0L
	
	private val secret = System.getenv("BITMEX_SECRET") ?: ""
	val apiID = System.getenv("BITMEX_API_ID") ?: ""

	init {
		configureJsonMapperBitmex()
		if (mode == Mode.TRADE) startKeepAliveJob()
	}
	
	private fun configureJsonMapperBitmex() {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
		mapper.enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
		mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
		mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
		
		/*// this dumb abstract button mixin is needed because of a really stupid error message... message is
		// com.fasterxml.jackson.databind.JsonMappingException: Conflicting setter definitions for property "mnemonic": javax.swing.AbstractButton#setMnemonic(1 params) vs javax.swing.AbstractButton#setMnemonic(1 params)
		// the error message disappears if I remove GUI, so it has something to do with javax.swing
		mapper.addMixIn(AbstractButton::class.java, MyAbstractButton::class.java)*/
		
		val injectableValues = InjectableValues.Std()
		injectableValues.addValue(Exchange::class.java, exchange)
		mapper.injectableValues = injectableValues
		
		val module = SimpleModule()
		module.addDeserializer(CurrencyPair::class.java, currencyPairDeserializer)
		module.addDeserializer(Book::class.java, exchange.bookRestDeserializer)
		module.addDeserializer(Instruments::class.java, exchange.instrumentsRestDeserializer)
		module.addSerializer(OrderRequest::class.java, BitmexOrderRequestSerializer())
		mapper.registerModule(module)
	}
	
	private fun startKeepAliveJob(): TimedAction {
		return TimedAction(interval = 7000,
		                   startingDelay = 30_000,
		                   restClient = this,
		                   block = {
			                   if (this.lastRequest.ago > 80_000) {
				                   executeRequest(getKeepAliveRequest(), Request(returnChannel = Channel(),
				                                                                 content = false,
				                                                                 timeStamp = System.currentTimeMillis()))
			                   }
		                   })
	}
	
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument): HttpGet {
		val sym = instrument.symbol
		return HttpGet(baseAddress + "/orderBook/L2?symbol=${sym}&depth=0.0")
	}
	
	fun getKeepAliveRequest() = getOrdersHttpRequest("XBTUSD")
	
	private fun getOrdersHttpRequest(symbol: String): HttpRequestBase {
		val endpoint = "/api/v1/order?symbol=$symbol&count=1&reverse=true"
		val get = HttpGet(baseAddress + endpoint)
		get.addSignature("GET", endpoint, "")
		return get
	}
	
	fun getPostHttpRequest(orderRequest: OrderRequest): HttpRequestBase {
		val endpoint = "/api/v1/order"
		val post = HttpPost(baseAddress + endpoint)
		val jsonNode = mapper.valueToTree<JsonNode>(orderRequest)
		val data = jsonNode.toString()
		post.entity = StringEntity(data)
		post.addSignature("POST", endpoint, data)
		return post
	}
	
	fun getPostBulkHttpRequest(orderRequest: OrderRequest): HttpRequestBase {
		val endpoint = "/api/v1/order/bulk"
		val post = HttpPost(baseAddress + endpoint)
		val jsonNode = mapper.valueToTree<JsonNode>(orderRequest)
		val data = jsonNode.toString()
		post.entity = StringEntity(data)
		post.addSignature("POST", endpoint, data)
		return post
	}
	
	fun getAmendBulkHttpRequest(orderRequest: OrderRequest): HttpRequestBase {
		val endpoint = "/api/v1/order/bulk"
		val put = HttpPut(baseAddress + endpoint)
		val jsonNode = mapper.valueToTree<JsonNode>(orderRequest)
		val data = jsonNode.toString()
		put.entity = StringEntity(data)
		put.addSignature("PUT", endpoint, data)
		return put
	}
	
	fun getCancelHttpRequest(orderRequest: OrderRequest): HttpRequestBase {
		val endpoint = "/api/v1/order"
		val delete = HttpDeleteWithBody(baseAddress + endpoint)
		val jsonNode = mapper.valueToTree<JsonNode>(orderRequest)
		val data = jsonNode.toString()
		delete.entity = StringEntity(data)
		delete.addSignature("DELETE", endpoint, data)
		return delete
	}
	
	// note that while we do not send our orders technically (json filters them out), we will get them back
	fun getCancelAllHttpRequest(orderRequest: OrderRequest): HttpEntityEnclosingRequestBase {
		val endpoint = "/api/v1/order/all"
		val delete = HttpDeleteWithBody(baseAddress + endpoint)
		val jsonNode = mapper.valueToTree<JsonNode>(orderRequest)
		val data = jsonNode.toString()
		if (data != "{}") {
			delete.entity = StringEntity(data)
			delete.addSignature("DELETE", endpoint, data)
		} else {
			delete.addSignature("DELETE", endpoint, "")
		}
		return delete
	}
	
	override fun getActiveInstrumentsHttpRequest(): HttpGet {
		val endpoint = "/api/v1/instrument/active"
		val httpGet = HttpGet(baseAddress + endpoint)
		httpGet.addSignature("GET", endpoint, "")
		return httpGet
	}
	
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("implement this")
	}
	
	
	
	
	
	/*fun executeOrders(httpRequest: HttpRequestBase, request: OrderChange): OrderChange {
		try {
			request.addEvent(event = Event.EXECUTING_START, time = now)
			val startReponseTime = now
			val httpResponse: CloseableHttpResponse = client.execute(httpRequest)
			val stopResponseTime = now
			val reqTime = stopResponseTime - startReponseTime
			val startOtherTime = now
			val statusCode: Int = httpResponse.statusLine.statusCode
			request.addEvent(event = if (statusCode.isError) Event.EXECUTING_END_ERROR else Event.EXECUTING_END, time = now)
			remaining = getRemainingRequests(httpResponse)
			
			
			// error
			if (statusCode.isError) {
				println("Error: ${httpResponse.statusLine.reasonPhrase}")
				val error = Error(code = statusCode,
				                  text = "couldn't ${request.requestType} orders on bitmex. Reason: ${EntityUtils.toString(httpResponse.entity)}")
				EntityUtils.consume(httpResponse.entity)
				httpResponse.close()
				return request.addError(error)
			// success
			} else {
				val startMappingTime = now
				val orderList: List<BitmexOrder> = mapper.readValue(httpResponse.entity.content, bitmexOrderTypeReference)
				val stopMappingTime = now
				val successfulRequest = request.swapOrders(orderList)
				successfulRequest.addEvent(Event.EXECUTING_END, now)
				val responseTime = stopResponseTime - startReponseTime
				// print out some stats
				if (responseTime > 700) {
					val mappingTime = stopMappingTime - startMappingTime
					val stopOtherTime = now
					val otherTime = (stopOtherTime - startOtherTime) - mappingTime
					println("req: $reqTime, other: $otherTime, mapping: $mappingTime")
				}
				EntityUtils.consume(httpResponse.entity)
				httpResponse.close()
				return successfulRequest
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
			return request.addError(Error(code = 400,
			                              text = "could not convert json response to orders for order request" +
					                              "${request.requestType} and orders  ${request.orders.joinToString(",")}"))
		}
	}*/
	
	override fun updateRateLimits(httpResponse: HttpResponse) {
		setRemainingRequests(httpResponse)
		if (httpResponse.statusLine.statusCode == 429) {
			httpResponse.getFirstHeader("Retry-After")?.let {
				retryAt = System.currentTimeMillis() + (it.value.toInt() * 1000L)
				log("rate-limited, please wait ${it.value.toInt()} seconds")
			}
		}
		printRemainingRequests()
	}
	
	fun printRemainingRequests() {
		if (lastPrintedRemaining.ago > 2000) {
				println("remaining requests: $remaining")
				lastPrintedRemaining = now
			}
	}
	
	fun setRemainingRequests(response: HttpResponse) {
		response.getFirstHeader("X-RateLimit-Remaining")?.let {
			remaining = it.value.toInt()
		}
	}
	
	private fun getRemainingRequests(response: HttpResponse): Int {
		val iter = response.headerIterator()
		while (iter.hasNext()) {
			val header = iter.nextHeader()
			if (header.name == "X-RateLimit-Remaining") {
				return header.value.toInt()
			}
		}
		return 600
	}
	
	// 70r-OeVUjAezE773qKqan8_a4VSrnLXSbagYt440w0XFiIPk
	// verb = "GET"
	// endpoint = "/realtime"
	// data as json string
	fun getSignature(verb: String, endpoint: String, nonce: Long, data: String = ""): String {
		val message = verb + endpoint + nonce.toString() + data
		val secretkey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
		mac.init(secretkey)
		return Hex.encodeHexString(mac.doFinal(message.toByteArray()))
	}
	
	fun HttpRequestBase.addSignature(verb: String, endpoint: String, data: String) {
		val nonce = exchange.nonce.incrementAndGet()
		this.addHeader("Accept", "application/json")
		this.addHeader("Content-type", "application/json")
		this.addHeader("api-nonce", nonce.toString())
		this.addHeader("api-key", apiID)
		this.addHeader("api-signature", getSignature(verb, endpoint, nonce, data))
	}
	
	
}