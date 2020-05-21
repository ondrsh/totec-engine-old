package tothrosch.networking.livefeed.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import tothrosch.engine.message.Message
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.instrument.Side
import tothrosch.instrument.Trade
import tothrosch.instrument.Trades
import tothrosch.instrument.book.*
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.networking.Heartbeat
import tothrosch.networking.livefeed.LiveFeedException
import tothrosch.networking.livefeed.WebsocketClient
import tothrosch.settings.Settings
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by ndrsh on 06.07.17.
 */

@WebSocket(maxTextMessageSize = 65536 * 1000)
open class BitfinexWebsocketDebugWrite(exchange: Exchange) : WebsocketClient(exchange) {
	
	override val address: String = "wss://api.bitfinex.com/ws"
	val mapper = ObjectMapper()
	val channelToInst: HashMap<Int, Instrument> = hashMapOf()
	// trades --> 1
	// book --> 2
	val channelToType: HashMap<Int, Int> = hashMapOf()
	val lastPing: Heartbeat = Heartbeat()
	val rootDirectory: String = Settings.dataPath + File.separator + "bitfinex"
	var file: File = File(rootDirectory + File.separator + "raw" + ".log")
	val writer = BufferedWriter(FileWriter(file.absoluteFile))
	var countNotFlushed = 0
	
	override suspend fun subscribe() {
		channelToInst.clear()
		channelToType.clear()
		for (sym: String in exchange.symToInst.keys) {
			subscribeBook(sym)
			subscribeTrades(sym)
		}
	}
	
	fun unsubscribeToInst(instrument: Instrument) {
		// first, get both chanIds (book and trade)
		val chanIds: List<Int> = channelToInst.filter { it.value == instrument }.keys.toList()
		for (chanId: Int in chanIds) {
			val request: ObjectNode = mapper.createObjectNode()
			request.put("event", "unsubscribeToInst")
			request.put("chanId", chanId)
			sendMessage(request.toString())
		}
	}
	
	fun subscribeBook(sym: String) {
		val request: ObjectNode = mapper.createObjectNode()
		request.put("event", "subscribe")
		request.put("channel", "book")
		request.put("pair", sym)
		request.put("prec", "P0")
		request.put("len", "100")
		sendMessage(request.toString())
	}
	
	fun subscribeTrades(sym: String) {
		val request: ObjectNode = mapper.createObjectNode()
		request.put("event", "subscribe")
		request.put("channel", "trades")
		request.put("pair", sym)
		sendMessage(request.toString())
	}
	
	
	override suspend fun processMessage(msg: String) {
		write(msg)
		if (lastPing.age() > 200_000) ping()
		val node: JsonNode = mapper.readValue(msg)
		if (node.get("event")?.asText() == "subscribed") addChannel(node)
		
		if (node.isArray) {
			val channel: Int = node.get(0).asInt()
			val instrument: Instrument = channelToInst[channel] ?: throw LiveFeedException(
				"Bitfinex websocket error - couldn't get instrument from chanId from message $msg"
			)
			val type: Int = channelToType[channel] ?: throw LiveFeedException(
				"Bitfinex websocket error -couldn't get channeltype from message $msg"
			)
			
			when (type) {
				1    -> processTradeMessage(node as ArrayNode, instrument)
				2    -> {
					if (node.size() == 2 && node.get(1).isArray) processFullBook(node.get(1) as ArrayNode, instrument)
					if (node.size() == 4 && node.isArray) processBookMessage(node as ArrayNode, instrument)
				}
				else -> throw LiveFeedException(
					"Bitfinex Websocket error - couldn't determine channel type of message $msg"
				)
			}
			
		}
	}
	
	suspend fun addChannel(node: JsonNode) {
		val chanId: Int = node.get("chanId").asInt()
		val type: String = node.get("channel").asText()
		val instrument: Instrument = exchange.symToInst[node.get("pair").asText()]!!
		
		channelToInst.put(chanId, instrument)
		channelToType.put(
			chanId, if (type == "trades") 1
			else if (type == "book") 2
			else throw LiveFeedException(
				"Bitfinex Websocket - couldn't determine channel type of message ${node.asText()}"
			)
		)
	}
	
	// [2353,[[2381.6,2,19.77626127],[2381.2,2,0.02043871],[2381,1,0.5],[2380.5,1,1.20028308],[2380.2,1,17],[2380.1,1,2],[2380,5,71.18039141],[2379.3,2,33.84292993],[2378.2,1,1.24557678],[2377.3,1,1.75],[2377.1,1,1.26822363],[2376.4,1,2.69],[2376.1,1,1.6627],[2376,1,1.29087048],[2375.5,1,0.46659843],[2375.4,1,1.47],[2375,2,0.6867],[2374.8,1,1.31351733],[2374.3,2,1.65],[2373.7,1,1.33616418],[2373.4,1,6.2572],[2373.2,1,1.29],[2373.1,3,7.47697201],[2372.9,1,0.06],[2372.7,1,3.5],[2372.6,1,1.35881104],[2372,1,3],[2371.8,1,0.1],[2371.5,1,1.38145789],[2371.3,1,1.96],[2371,1,9],[2370.8,1,0.013],[2370.4,1,1.40835102],[2369.9,1,0.012658],[2369.8,1,1.5109],[2369.7,1,0.298113],[2369.2,1,1.43665958],[2369,1,0.533274],[2368.9,1,1.2301],[2368.1,2,2.65711015],[2367.1,2,3.63795983],[2367,1,1.49327671],[2366.2,1,1],[2366,1,1.4],[2365.9,1,1.52158527],[2365.2,1,1],[2365.1,1,10],[2365,1,3.48],[2364.9,1,0.15],[2364.7,3,4.54989384],[2364.3,1,1.20975993],[2363.6,1,1.5782024],[2363.3,1,1.3],[2363.1,1,1],[2363,1,2.399],[2362.6,1,1.26356055],[2362.5,1,1.60651096],[2361.6,1,1.2756],[2361.4,1,1.63481953],[2361.1,1,4.1614],[2361,1,9],[2360.9,1,0.110664],[2360.7,1,1.4],[2360.3,1,1.66312809],[2360,7,19.00999999],[2359.7,1,1],[2359.1,1,1.69143665],[2359,1,1.35617481],[2358.2,1,1.3392],[2358,1,1.71974522],[2357.5,1,0.94],[2357.2,1,1.2],[2356.9,1,1.76786978],[2356.3,1,1.34533921],[2356.2,1,2],[2355.8,1,1.83581033],[2355.5,1,9.22],[2355.4,1,1.5],[2355,1,0.4],[2354.7,1,0.70521588],[2354.6,1,1.90375088],[2354.2,2,4.119413],[2353.5,1,1.97169143],[2353.3,1,1],[2352.4,1,2.03963198],[2352.3,1,0.925],[2352.1,1,1.5],[2352,1,0.05],[2351.5,1,4.8],[2351.4,1,10.36],[2351.3,1,2.10757254],[2351.2,1,1.48646334],[2351.1,1,0.13],[2351,1,9],[2350.4,1,30],[2350.3,1,1.6],[2350.2,1,2.17551309],[2350,1,49.9],[2349.6,1,0.0248225],[2349.2,1,1.60284817],[2381.7,2,-0.6889],[2382.3,1,-1.7632],[2383.1,2,-1.06037544],[2383.2,1,-0.886],[2383.3,1,-2],[2384.3,1,-1.5232],[2385.1,1,-1.38],[2385.2,1,-1.54],[2386.2,2,-9.8753],[2386.4,1,-1.405528],[2386.7,1,-0.285731],[2387.1,1,-0.06],[2387.2,1,-1.5293],[2388.3,1,-1],[2389.3,1,-1.388346],[2389.8,2,-1.02475282],[2390,1,-2.01859118],[2390.3,1,-1.4562],[2391.3,1,-2],[2392.4,1,-1.395],[2393.2,1,-1.4046],[2394.3,1,-1.48],[2395.2,1,-2],[2395.6,1,-0.01],[2396.2,1,-1.51107717],[2397.1,1,-1.43],[2398,1,-2.8275563],[2398.1,1,-1.51560308],[2398.2,1,-0.67573207],[2398.3,1,-2.39907363],[2398.9,1,-0.04],[2399,1,-1],[2399.9,1,-1.4],[2400,1,-5],[2400.7,2,-21.521676],[2401.6,1,-0.7004717],[2401.8,1,-1],[2402,1,-4.1646],[2402.1,1,-2.21645331],[2402.6,2,-31],[2402.9,2,-3.96045937],[2403,1,-0.1],[2403.4,1,-1.2965],[2403.9,1,-17],[2404,1,-0.2],[2404.2,1,-1.329824],[2405,1,-1.18],[2405.1,1,-3.09],[2405.9,2,-4.287456],[2406.6,1,-0.38086936],[2406.7,1,-1.3],[2407,1,-0.05366337],[2407.4,1,-4],[2407.5,1,-1],[2407.8,1,-5.38546159],[2408.2,1,-1.2401],[2408.7,3,-1.53378057],[2408.8,1,-0.01269643],[2408.9,1,-4.8],[2409.1,1,-1.198467],[2409.7,1,-3],[2409.8,2,-1.42117],[2410,1,-0.66710611],[2410.5,1,-0.06085813],[2410.6,1,-1.3922],[2410.7,1,-20],[2411.3,1,-3],[2411.4,1,-1.38993614],[2412.6,1,-3],[2412.8,2,-5.757],[2413.1,2,-1.51],[2413.9,2,-20],[2414,1,-5],[2414.3,1,-1.2270053],[2414.4,1,-1.51],[2414.9,1,-0.25],[2415,1,-10],[2416.2,1,-0.04],[2417,1,-4.8],[2417.2,1,-0.974],[2418,1,-0.4],[2418.5,1,-0.032639],[2419,3,-6.56142745],[2420,4,-23],[2421,1,-4.8],[2421.4,1,-2],[2422.4,1,-0.992],[2422.6,1,-2],[2423.1,1,-1.31530537],[2423.3,1,-5],[2423.4,2,-5.49899857],[2423.8,1,-2],[2424.6,1,-4.8],[2424.9,1,-4.8],[2425,2,-27],[2426,1,-0.1],[2427.6,1,-1.01],[2428.5,1,-0.15],[2428.7,1,-2],[2429,1,-5]]]
	suspend fun processFullBook(jsonArray: ArrayNode, instrument: Instrument) {
		//log("sending snapshot at time ${System.currentTimeMillis()}")
		
		
		val bidsMap: HashMap<String, BookEntry> = HashMap(jsonArray
			                                                  .filter {
				                                                  it.get(2).asDouble() > 0
			                                                  }.associate {
				it.get(0).asDouble().toString() to BookEntry(
					it.get(0).asDouble(), it.get(2).asDouble(),
					it.get(0).asDouble().toString()
				)
			})
		
		
		val asksMap: HashMap<String, BookEntry> = HashMap(jsonArray
			                                                  .filter {
				                                                  it.get(2).asDouble() < 0
			                                                  }.associate {
				it.get(0).asDouble().toString() to BookEntry(
					it.get(0).asDouble(), -1 * it.get(2).asDouble(),
					it.get(0).asDouble().toString()
				)
			})
		
		
		val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
		val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
		val book = Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet))
		instrument.channel.send(Message(book))
	}
	
	
	// orderbook update: [chanId, price, number of orders, amount]
	// side (ask or bid) is determined by sign of the amount
	// third entry is usually ignored by my code, except when it is 0, because then order will be deleted
	// [30005,2414,2,-5.2]              -->     channel 30005, ask at price 2414 now has 2 orders with amount = 5.2
	// [30005,2413.9,0,-1]              -->     channel 30005, ask at price 2413.9 is deleted from asks (ask, because amount is negative)
	// [30005,2358.2,1,0.69129356]      -->     channel 30005, bid at price 2358.2 now has amount 0.69129356 with 1 order
	// Noteworthy: no difference between adding or changing from incoming messages POV --> make this yourself in instrument
	suspend fun processBookMessage(jsonArray: ArrayNode, instrument: Instrument) {
		val price: Double = jsonArray.get(1).asDouble()
		val numberOfOrders: Int = jsonArray.get(2).asInt()
		val amountMaybeNegative: Double = jsonArray.get(3).asDouble()
		val amount: Double = Math.abs(amountMaybeNegative)
		val side: Side = when (Math.signum(amountMaybeNegative)) {
			1.0  -> Side.BUY
			-1.0 -> Side.SELL
			else -> throw LiveFeedException("Bitfinex Websocket error - amount can't be 0.0. Message: $jsonArray")
		}
		val bookEntry = BookEntry(price, amount, price.toString())
		if (numberOfOrders == 0) instrument.channel.send(
			Message(BookOperations(listOf(BookOperation.Delete(side, bookEntry))))
		)
		else instrument.channel.send(Message(BookOperations(listOf(BookOperation.Insert(side, bookEntry)))))
	}
	
	// [65,"te","8429617-BTCUSD",1499937894,2341.4,0.01]
	// [65,"tu","8429617-BTCUSD",43238484,1499937894,2341.4,0.01]
	// [65,"te","8429618-BTCUSD",1499937912,2341.5,0.21367198]
	// [65,"tu","8429618-BTCUSD",43238496,1499937912,2341.5,0.21367198]
	suspend fun processTradeMessage(jsonArray: ArrayNode, instrument: Instrument) {
		if (jsonArray.get(1).asText() != "te") return
		val price: Double = jsonArray.get(4).asDouble()
		val amountMaybeNegative: Double = jsonArray.get(5).asDouble()
		val amount: Double = Math.abs(amountMaybeNegative)
		val side: Side = when (Math.signum(amountMaybeNegative)) {
			1.0  -> Side.BUY
			-1.0 -> Side.SELL
			else -> throw LiveFeedException("Bitfinex Websocket error - amount can't be 0.0. Message: $jsonArray")
		}
		
		instrument.channel.send(Message(Trades(listOf(Trade(price = price,
		                                                    amount = amount,
		                                                    initiatingSide = side)))))
	}
	
	
	suspend fun ping() {
		sendMessage("""{"event":"ping"}""")
		lastPing.update()
	}
	
	fun write(msg: String) {
		writer.write(now.toString() + "," + msg + System.lineSeparator())
		countNotFlushed++
		if (countNotFlushed > 500) {
			writer.flush()
			countNotFlushed = 0
		}
	}
	
}