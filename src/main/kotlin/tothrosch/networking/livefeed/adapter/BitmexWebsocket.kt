package tothrosch.networking.livefeed.adapter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import tothrosch.engine.Keep
import tothrosch.engine.message.Message
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.*
import tothrosch.instrument.book.*
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.json.CurrencyPairDeserializer
import tothrosch.networking.livefeed.LiveFeedException
import tothrosch.networking.livefeed.WebsocketClient
import tothrosch.networking.rest.adapter.bitmex.BitmexRestClient
import tothrosch.trading.Hub
import tothrosch.trading.orders.BitmexOrder
import tothrosch.trading.orders.BitmexOrders
import tothrosch.trading.position.bitmex.BitmexPosition
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by ndrsh on 28.05.17.
 */

@WebSocket(maxTextMessageSize = 65536 * 1000)
class BitmexWebsocket : WebsocketClient(Exchange.BitmexExchange) {
	override val address: String = "wss://www.bitmex.com/realtime"
	val mapper = jacksonObjectMapper()
	private val liqIdToSide: HashMap<String, Side> = hashMapOf()
	private var lastMsg: Long = 0
	
	init {
		mapper.enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
		val module = SimpleModule()
		module.addDeserializer(CurrencyPair::class.java, CurrencyPairDeserializer(exchange))
		module.addDeserializer(Instruments::class.java, exchange.instrumentsRestDeserializer)
		mapper.registerModule(module)
	}
	
	override suspend fun subscribe() {
		authenticate()
		/* for (instrument in globalInstruments) {
			 subscribeIndividual(instrument)
		 }*/
		subscribeGeneral()
	}
	
	private fun subscribeGeneral() {
		val request = mapper.createObjectNode()
		request.put("op", "subscribe")
		request.replace("args", getGeneralSubscriptionArray())
		sendMessage(request.toString())
		println("request: $request")
	}
	
	private fun getGeneralSubscriptionArray(): ArrayNode {
		val subArray = mapper.createArrayNode()
		subArray.add("orderBookL2")
		subArray.add("orderBook10")
		subArray.add("trade")
		// subArray.add("position")
		// subArray.add("settlement")
		// subArray.add("order")
		subArray.add("instrument")
		subArray.add("liquidation")
		return subArray
	}
	
	/*	private fun getIndividualSubscriptionArray(instrument: Instrument): ArrayNode {
			val subArray = mapper.createArrayNode()
			val symbol = instrument.symbol
			
			subArray.add("orderBookL2:$symbol")
			subArray.add("trade:$symbol")
			
			return subArray
		}*/
	
	// {"op": "authKey", "args": ["<APIKey>", <nonce>, "<signature>"]}
	private fun authenticate() {
		println("authenticating bitmex websocket")
		val authenticationNode = mapper.createObjectNode()
		authenticationNode.put("op", "authKey")
		
		val nonce = exchange.nonce.incrementAndGet()
		val argNode = mapper.createArrayNode()
		argNode.add((exchange.restClient as BitmexRestClient).apiID)
		argNode.add(nonce)
		argNode.add((exchange.restClient as BitmexRestClient).getSignature("GET", "/realtime", nonce))
		
		authenticationNode.replace("args", argNode)
		sendMessage(authenticationNode.toString())
		println("sent authentication node: $authenticationNode")
	}
	
	
	override suspend fun processMessage(msg: String) {
		// log(System.currentTimeMillis(), msg)
		val node: JsonNode = mapper.readTree(msg)
/*		when (node.get("table")?.asText()) {
			"orderBook10", "orderBookL2", "trade" -> {
				val nowTime = now
				val timeToLast = nowTime - lastMsg
				println("$timeToLast $msg")
				lastMsg = nowTime
			}
		}*/
		when (node.get("table")?.asText()) {
			"orderBookL2" -> processBookMessage(node)
			"trade"       -> processTradeMessage(node)
			"instrument"  -> processInstrumentMessage(node)
			"order"       -> processOrders(node)
			"position"    -> processPositions(node)
			"liquidation" -> processLiquidations(node)
		}
		
	}
	
	private suspend fun processBookMessage(node: JsonNode) {
		val (data, action) = getDataAndAction(node)
		
		// TODO drop messages that arrive before partial
		when (action) {
			"partial" -> processFullBooks(data)
			else      -> processBookOperations(action, data, false)
		}
	}
	
	private suspend fun processFullBooks(data: JsonNode) {
		val nodeMapBySymbol = groupNodesByInst(data)
		for (entry in nodeMapBySymbol) {
			processFullSymBook(entry.key, entry.value)
		}
	}
	
	private suspend fun processFullSymBook(instrument: Instrument, jsonNodes: List<JsonNode>) {
		try {
			val dataBySide: Map<String, List<JsonNode>> = jsonNodes.groupBy { it.get("side").asText() }
			val bidsMap =
				HashMap<String, BookEntry>(
					dataBySide["Buy"]?.associate { it.get("id").asText() to createBookEntry(it) })
			val asksMap =
				HashMap<String, BookEntry>(
					dataBySide["Sell"]?.associate { it.get("id").asText() to createBookEntry(it) })
			val bidsSet = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			val book = Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet), time = now)
			instrument.channel.send(Message(book, timestamp = book.time))
		} catch (ex: Exception) {
			ex.printStackTrace()
			
		}
		
	}
	
	private suspend fun processBookOperations(action: String, data: JsonNode, isLiquidation: Boolean) {
		val nodeMapBySymbol = groupNodesByInst(data)
		for (entry in nodeMapBySymbol) {
			processSymBookOperations(action, entry.key, entry.value, isLiquidation)
		}
	}
	
	private suspend fun processSymBookOperations(action: String, inst: Instrument, jsonNodes: List<JsonNode>, isLiquidation: Boolean) {
		// Liquidations
		if (isLiquidation) {
			// println("creating liquidation)")
			when (action) {
				"insert" -> {
					val ops = jsonNodes.map {
						val side = getSide(it["side"].asText())
						val id = it["orderID"].asText()
						liqIdToSide[id] = side
						BookOperation.Insert(side = side,
						                     bookEntry = createLiquidationEntry(it))
					}
					inst.channel.send(Message(BookOperations(ops)))
				}
				"update" -> {
					val ops = jsonNodes.mapNotNull {
						val id = it["orderID"].asText()
						val side = liqIdToSide[id]
						if (side == null) {
							println("couldn't find liquidation in hashmap")
							return@mapNotNull null
						}
						BookOperation.Insert(side = side,
						                     bookEntry = createLiquidationEntry(it))
					}
					if (ops.isNotEmpty()) {
						inst.channel.send(Message(BookOperations(ops)))
					}
				}
				
				"delete" -> {
					val ops = jsonNodes.mapNotNull {
						val id = it["orderID"].asText()
						val side = liqIdToSide[id]
						if (side == null) {
							println("couldn't find liquidation in hashmap")
							return@mapNotNull null
						}
						BookOperation.Delete(side = side,
						                     bookEntry = createLiquidationEntry(it))
					}
					if (ops.isNotEmpty()) {
						inst.channel.send(Message(BookOperations(ops)))
					}
					ops.forEach { liqIdToSide.remove(it.bookEntry.id) }
				}
				else     -> throw LiveFeedException(
					"BitmexWebsocket error - trying to process book operation, but action has value $action - don't know what that is")
			}
		}
		// normal updates
		else {
			when (action) {
				"insert", "update" -> inst.channel.send(Message(BookOperations(jsonNodes.map {
					BookOperation.Insert(side = getSide(it.get("side").asText()),
					                     bookEntry = createBookEntry(it))
				})))
				"delete"           -> inst.channel.send(Message(BookOperations(jsonNodes.map {
					BookOperation.Delete(side = getSide(it.get("side").asText()),
					                     bookEntry = createBookEntry(it))
				})))
				else               -> throw LiveFeedException(
					"BitmexWebsocket error - trying to process book operation, but action has value $action - don't know what that is")
			}
		}
		
	}
	
	private suspend fun processLiquidations(node: JsonNode) {
		// println(node.toString())
		val (data, action) = getDataAndAction(node)
		processBookOperations(action, data, true)
	}
	
	private fun getDataAndAction(node: JsonNode): Pair<JsonNode, String> {
		val data: JsonNode = node.get("data") ?: throw LiveFeedException(
			"BitmexWebsocket error - trying to process book message but no field \"data\", full message is ${node.asText()}")
		val action = node.get("action")?.asText() ?: throw LiveFeedException(
			"BitmexWebsocket error - trying to process message but no field \"action\", full message is ${node.asText()}")
		return data to action
	}
	
	private suspend fun processTradeMessage(node: JsonNode) {
		if (node.get("action").asText() != "insert") return
		val data = node.get("data")
		val nodeMapBySymbol = groupNodesByInst(data)
		
		for (entry in nodeMapBySymbol) {
			entry.key.channel.send(Message(Trades(entry.value.map {
				Trade(price = it.get("price").asDouble(),
				      amount = it.get("size").asDouble(),
				      initiatingSide = getSide(it.get("side").asText()))
			})))
		}
		
		//{"table":"trade","action":"insert","data":[{"timestamp":"2017-06-14T20:53:35.142Z","symbol":"XBTUSD","side":"Sell","size":3500,"price":2547.1,"tickDirection":"MinusTick","trdMatchID":"58664baf-dd1d-aa6b-2cdf-9c2bf3cdb8df","grossValue":137410000,"homeNotional":1.3741,"foreignNotional":3500},{"timestamp":"2017-06-14T20:53:35.142Z","symbol":"XBTUSD","side":"Sell","size":2000,"price":2547.1,"tickDirection":"ZeroMinusTick","trdMatchID":"bc7201a2-f774-da5b-b336-cda7102ae556","grossValue":78520000,"homeNotional":0.7852,"foreignNotional":2000},{"timestamp":"2017-06-14T20:53:35.142Z","symbol":"XBTUSD","side":"Sell","size":1000,"price":2547.1,"tickDirection":"ZeroMinusTick","trdMatchID":"2602b494-8e10-119b-f700-db7dbcdb4c26","grossValue":39260000,"homeNotional":0.3926,"foreignNotional":1000},{"timestamp":"2017-06-14T20:53:35.142Z","symbol":"XBTUSD","side":"Sell","size":8511,"price":2547.1,"tickDirection":"ZeroMinusTick","trdMatchID":"85e2276d-9764-e415-67f7-1778d095c83d","grossValue":334141860,"homeNotional":3.3414186,"foreignNotional":8511}]}
	}
	
	private fun createBookEntry(node: JsonNode) = BookEntry(price = node.get("price")?.asDouble() ?: 0.0,
	                                                        amount = node.get("size")?.asDouble() ?: 0.0,
	                                                        id = node.get("id").asText())
	
	private fun createLiquidationEntry(node: JsonNode) = BookEntry(price = node.get("price")?.asDouble() ?: 0.0,
	                                                               amount = node.get("leavesQty")?.asDouble() ?: 0.0,
	                                                               id = node.get("orderID").asText(),
	                                                               isLiquidation = true)
	
	private suspend fun processOrders(node: JsonNode) {
		val data: ArrayNode = node.get("data") as ArrayNode
		val orders = BitmexOrders(data.map { mapper.treeToValue<BitmexOrder>(it)!! })
		Hub.orderHandler.send(Message(orders))
	}
	
	private suspend fun processPositions(node: JsonNode) {
		val data: ArrayNode = node.get("data") as ArrayNode
		data.forEach { processPosition(it) }
	}
	
	private suspend fun processPosition(node: JsonNode) {
		val position: BitmexPosition = mapper.treeToValue(node)!!
		Hub.positionHandler.send(Message(position))
	}
	
	// TODO take care of instruments that get inactive... delete them somehow... lol good luck
	private suspend fun processInstrumentMessage(node: JsonNode) {
		val (data, action) = getDataAndAction(node)
		val instrumentDataByInst = groupNodesByInst(data)
		if (action == "insert" || action == "delete") println(node.toString())
		for ((inst, instData) in instrumentDataByInst) {
			val update: Update = mapper.createObjectNode()
			for (jsonUpdate in instData) {
				for ((key, value) in jsonUpdate.fields()) {
					if (key != "timestamp" &&
						value.asText() != "null" &&
						value.asText().isNotBlank()
					) {
						update.put(key, value.asText())
					}
					
				}
			}
			inst.channel.send(Message(update))
		}
	}
	
	/*	fun flattenJson(jsonNodes: List<JsonNode>): JsonNode {
			val flattenedNode = mapper.createObjectNode()
			for (jsonNode in jsonNodes) {
				for ((entry, value) in jsonNode.fields()) {
					flattenedNode.set(entry, value)
				}
			}
			return flattenedNode
		}*/
	
	private fun getSide(sideAsString: String): Side =
		when (sideAsString) {
			"Buy"  -> Side.BUY
			"Sell" -> Side.SELL
			else   -> throw LiveFeedException("Bitmex networking error - couldn't recognize side $sideAsString")
		}
	
	private fun groupNodesByInst(node: JsonNode): HashMap<Instrument, ArrayList<JsonNode>> {
		val nodeMapBySymbol = hashMapOf<Instrument, ArrayList<JsonNode>>()
		
		for (individualNode in node) {
			val sym = individualNode.get("symbol").asText()
			// filter out the indices
			if (sym.first() != '.') {
				val inst = getInstFromSym(sym) ?: continue
				if (nodeMapBySymbol.containsKey(inst) == false) {
					nodeMapBySymbol[inst] = arrayListOf(individualNode)
				} else {
					nodeMapBySymbol[inst]!!.add(individualNode)
				}
			}
		}
		
		return nodeMapBySymbol
	}
	
	private fun getInstFromSym(sym: String): Instrument? = exchange.symToInst[sym]
}