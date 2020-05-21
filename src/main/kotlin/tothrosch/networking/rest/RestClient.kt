package tothrosch.networking.rest

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.http.HttpResponse
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.util.EntityUtils
import tothrosch.engine.message.Error
import tothrosch.engine.message.OrderRequest
import tothrosch.engine.message.Request
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.Instrument
import tothrosch.instrument.Instruments
import tothrosch.instrument.book.Book
import tothrosch.json.CurrencyPairDeserializer
import tothrosch.util.execute
import tothrosch.util.isError
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import tothrosch.util.time.log.Event

/**
 * Created by ndrsh on 30.06.17.
 */

abstract class RestClient(val exchange: Exchange): CoroutineScope by exchange, TimeScope {
	override val time: Time = Time.Live
	open val restBookDelay: Long = 450
	@Volatile var lastRequest: Long = 0L
	val httpAsyncClient: CloseableHttpAsyncClient = HttpAsyncClients.custom()
		.setDefaultRequestConfig(RequestConfig.custom()
			                         .setCookieSpec(CookieSpecs.STANDARD)
			                         .build())
		.setMaxConnPerRoute(100)
		.setMaxConnTotal(100)
		.build()
	val mapper = jacksonObjectMapper()
	val currencyPairDeserializer = CurrencyPairDeserializer(exchange)
	
	init {
		configureJsonMapperBase()
	}
	
	private fun configureJsonMapperBase() {
		val module = SimpleModule()
		module.addDeserializer(CurrencyPair::class.java, currencyPairDeserializer)
		module.addDeserializer(Book::class.java, exchange.bookRestDeserializer)
		module.addDeserializer(Instruments::class.java, exchange.instrumentsRestDeserializer)
		mapper.registerModule(module)
	}
	
	inline fun <reified T> executeRequest(http: HttpRequestBase, request: Request<T>, keepAlive: Boolean = false) = launch {
		var httpResponse: HttpResponse? = null
		try {
			request.addEvent(Event.REST_EXECUTING_START)
			if (Book::class.java is T) delay(restBookDelay)
			httpResponse = httpAsyncClient.execute(http)
			lastRequest = now
			if (keepAlive) {
				httpResponse.entity?.let { EntityUtils.consume(it) }
				return@launch
			}
			val statusCode: Int = httpResponse.statusLine.statusCode
			request.addEvent(if (statusCode.isError) Event.REST_EXECUTING_END_ERROR else Event.REST_EXECUTING_END)
			
			
			if (request is OrderRequest) updateRateLimits(httpResponse)
			
			// ERROR
			if (statusCode.isError) {
				println("Error: ${httpResponse.statusLine.reasonPhrase}")
				request.error = getError(httpResponse, request)
				request.addEvent(Event.REST_EXECUTING_END_ERROR)
				request.returnChannel.send(request)
				
			// SUCCESS
			} else {
				/*val content = if (T::class.java == BitmexOrders::class.java) {
						request.content as BitmexOrders
						val newOrders = mapper.readValue<List<BitmexOrder>>(httpResponse.entity.content)
						request.base.swapOrders(newOrders) as T
					} else {*/
				
				request.content = mapper.readValue(httpResponse.entity.content)
				request.addEvent(Event.REST_MAPPING_END)
				request.returnChannel.send(request)
			}
		// EXCEPTION
		} catch (ex: Exception) {
			ex.printStackTrace()
			request.error = getError(httpResponse, request, ex.toString())
			request.addEvent(Event.REST_EXECUTING_END_ERROR)
			request.returnChannel.send(request)
		} finally {
			httpResponse?.entity?.let { EntityUtils.consume(it) }
		}
	}
	
	// implement this in BitmexRestClient
	open fun updateRateLimits(httpResponse: HttpResponse) {}
	
	inline fun <reified T> getError(httpResponse: HttpResponse?, request: Request<T>, exceptionString: String? = null) = when (request) {
		is OrderRequest -> {
			if (exceptionString != null) {
				Error(code = 400,
				      text = "couldn't ${request.orderEvent} orders on bitmex. Exception: $exceptionString")
				
			} else {
				if (httpResponse != null) {
					Error(code = 400,
					      text = "couldn't ${request.orderEvent} orders on bitmex. Reason: ${EntityUtils.toString(httpResponse.entity)}")
				} else {
					Error(code = 400,
					      text = "couldn't ${request.orderEvent} orders on bitmex. Reason: $")
				}
			}
		}
		else            -> {
			Error(code = 400,
			      text = "could fulfil request for ${T::class.java} on restclient ${exchange.name}: " + exceptionString)
			
		}
	}
	
	/*inline fun <reified T> executeRequest(httpGet: HttpGet, request: Request<T>): Message<T> = async {
		try {
			// println("executing TRADE request for pair ${request.instrument.pair}")
			
			val resp = test.get {
			
			}
			val httpResponse: HttpResponse = client.execute(httpGet)
			val statusCode: Int = httpResponse.statusLine.statusCode
			if (statusCode.isError) {
				println("Error: ${httpResponse.statusLine.reasonPhrase}")
				val errorMessage = Message(
					content = null,
					error = Error.Request(
						statusCode,
						"couldn't retrieve object ${T::class.java} on ${exchange.name}",
						request
					),
					type = ConnectionType.REST
				)
				EntityUtils.consume(httpResponse.entity)
				// httpResponse.close()
				return errorMessage
			} else {
				val successMessage = Message(
					content = mapper.readValue(httpResponse.entity.content, T::class.java),
					type = ConnectionType.REST
				)
				EntityUtils.consume(httpResponse.entity)
				// httpResponse.close()
				return successMessage
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
			return Message(
				content = null,
				error = Error.Request(
					code = 400,
					text = "could not convert json response to object ${T::class.java} on ${exchange.name} : " + ex.toString(),
					originalRequest = request
				),
				type = ConnectionType.REST
			)
		}
	}*/
	
	abstract fun getBookSnapshotHttpRequest(instrument: Instrument): HttpGet
	abstract fun getTradesHttpRequest(instrument: Instrument): HttpGet
	abstract fun getActiveInstrumentsHttpRequest(): HttpGet
}
