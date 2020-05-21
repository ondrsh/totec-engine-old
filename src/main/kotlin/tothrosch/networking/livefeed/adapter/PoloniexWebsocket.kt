package tothrosch.networking.livefeed.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import tothrosch.networking.livefeed.LiveFeedException
import tothrosch.networking.livefeed.WebsocketClient
import tothrosch.settings.Settings
import java.util.*


/**
 * Created by ndrsh on 06.07.17.
 */

@WebSocket(maxTextMessageSize = 65536 * 1000)
class PoloniexWebsocket(exchange: Exchange) : WebsocketClient(exchange) {
	
	override val address: String by lazy { "wss://api2.poloniex.com" }
	val mapper = ObjectMapper()
	val channelToInst: HashMap<Int, Instrument> = hashMapOf()
	
	
	override suspend fun subscribe() {
		channelToInst.clear()
		for (instrument: Instrument in exchange.instruments) {
			val request = mapper.createObjectNode()
			request.put("command", "subscribe")
			request.put("channel", instrument.symbol)
			sendMessage(request.toString())
		}
	}
	
	
	override suspend fun processMessage(msg: String) {
		val node: JsonNode = mapper.readValue(msg)
		if (node.size() == 3 && node.get(2).get(0).size() == 2 && node.get(2).get(0).get(0).asText() == "i") {
			val chanId: Int = node.get(0).asInt()
			registerChannel(node, chanId)
			processFullBook(node, channelToInst[chanId]!!)
			return
		}
		if (node.size() == 3) {
			val instrument: Instrument = channelToInst[node.get(0).asInt()] ?: throw LiveFeedException("symbol of node $node was not in polo chanIdToInstrument map")
			val sequence: Long = node.get(1).asLong()
			// "o" = orderbook
			val bookJsons: List<JsonNode> = node.get(2).filter { it.get(0).asText() == "o" }
			// "t" = trade
			val tradeJsons: List<JsonNode> = node.get(2).filter { it.get(0).asText() == "t" }
			if (bookJsons.isNotEmpty()) processBookMessage(bookJsons, instrument, sequence)
			if (tradeJsons.isNotEmpty()) processTradeMessage(tradeJsons, instrument, sequence)
			
		} else {
			// log(msg)
		}
		
	}
	
	suspend fun registerChannel(node: JsonNode, chanId: Int) {
		val sym: String = node.get(2).get(0).get(1).get("currencyPair").asText()
		channelToInst.put(chanId, exchange.symToInst[sym]!!)
	}
	
	//[188,3950633,[["i",{"currencyPair":"ETH_GNO","orderBook":[{"0.89831797":"0.00012665","0.89944250":"0.00017370","0.90536307":"0.03618110","0.90651640":"0.02440000","20000.00000000":"1.00100000","30000.00000000":"1.00100000","40000.00000000":"1.00100000","49000.00000000":"0.00100000","49999.00000000":"1.00000000"},{"0.88495159":"0.23583548","0.88381584":"0.02590000","0.00000101":"10000.00000000","0.00000100":"2000.00000000","0.00000020":"50000.00000000","0.00000011":"100000.00000000","0.00000010":"20000.00000000","0.00000003":"100000.00000000","0.00000001":"209999.00000000"}]}]]]
	suspend fun processFullBook(node: JsonNode, instrument: Instrument) {
		val seq = node.get(1).asLong()
		
		val maps: MutableList<HashMap<String, BookEntry>> = arrayListOf()
		val sets: MutableList<SortedSet<BookEntry>> = arrayListOf()
		
		// first asks, then bids
		for (j in 0..1) {
			val bookNode: JsonNode = node.get(2).get(0).get(1).get("orderBook").get(j)
			var count = 0
			maps.add(hashMapOf())
			val mapToPut: HashMap<String, BookEntry> = maps[j]
			for ((key, value) in bookNode.fields()) {
				if (count < Settings.bookArraySize) {
					mapToPut.put(key, BookEntry(key.toDouble(), value.asDouble(), key))
					count++
				}
			}
			sets.add(mapToPut.values.toSortedSet(if (j == 0) AsksComparator else BidsComparator))
		}
		
		val book = Book(BidsMutable(maps[1], sets[1]), AsksMutable(maps[0], sets[0]), seq)
		instrument.channel.send(Message(book))
	}
	
	// [chanId, time, [["orderbook", 0=ask, 1=bid, amount]]]
	// we only get the arrays with "o"
	// [173,270275061,[["o",1,"15.09430003","0.00000000"],["o",1,"15.09430000","486.07256381"],["t","931798",0,"15.09430003","2.55741068",1500472441],["t","931799",0,"15.09430000","19.23058932",1500472441]]]
	// [148,375646583,[["o",1,"0.09427017","9.86898486"],["t","30764828",0,"0.09427017","0.10601514",1500472442]]]
	// [148,375646584,[["o",0,"0.09433829","25.55067901"],["t","30764829",1,"0.09433829","0.00000003",1500472442]]]
	suspend fun processBookMessage(bookJsons: List<JsonNode>, instrument: Instrument, sequence: Long) {
		val bookOps: List<BookOperation> = bookJsons.map { jsonToBookOperation(it) }
		instrument.channel.send(Message(BookOperations(bookOps, sequence)))
	}
	
	suspend fun jsonToBookOperation(node: JsonNode): BookOperation {
		val side: Side = when (node.get(1).asInt()) {
			0    -> Side.SELL
			1    -> Side.BUY
			else -> throw LiveFeedException("could not get side from polo json bookop: $node")
		}
		val price: Double = node.get(2).asDouble()
		val id: String = price.toString()
		val amount: Double = node.get(3).asDouble()
		val entry = BookEntry(price, amount, id)
		
		return if (amount == 0.0) BookOperation.Delete(side, entry)
		else BookOperation.Insert(side, entry)
	}
	
	// we only get the arrays with "t"
	// [173,270275061,[["t","931798",0,"15.09430003","2.55741068",1500472441],["t","931799",0,"15.09430000","19.23058932",1500472441]]]
	suspend fun processTradeMessage(tradesJsons: List<JsonNode>, instrument: Instrument, sequence: Long) {
		val trades: List<Trade> = tradesJsons.map { jsonToTrade(it) }
		val tradeMessage: Message<Any> = Message(Trades(trades = trades, sequence = sequence))
		instrument.channel.send(tradeMessage)
	}
	
	suspend fun jsonToTrade(node: JsonNode): Trade {
		val side: Side = when (node.get(2).asInt()) {
			0    -> Side.SELL
			1    -> Side.BUY
			else -> throw LiveFeedException("could not get side from polo json trade: $node")
		}
		val price: Double = node.get(3).asDouble()
		val amount: Double = node.get(4).asDouble()
		return Trade(price = price,
		             amount = amount,
		             initiatingSide = side)
	}
	
	
}