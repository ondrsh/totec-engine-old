package tothrosch.networking.livefeed.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import tothrosch.engine.message.Message
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.instrument.Side
import tothrosch.instrument.Trade
import tothrosch.instrument.Trades
import tothrosch.instrument.book.BookEntry
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.networking.livefeed.WebsocketClient


@WebSocket(maxTextMessageSize = 65536 * 1000)
class BinanceWebsocket: WebsocketClient(Exchange.BinanceExchange) {
	override val address by lazy { getBinanceStreamAddress() }
	val mapper = jacksonObjectMapper()
	
	override suspend fun processMessage(msg: String) {
		val node: JsonNode = mapper.readValue(msg)
		val streamNameSplit = node.get("stream").asText().split("@")
		val sym = streamNameSplit[0].toUpperCase()
		val instrument = exchange.symToInst[sym] ?: throw RuntimeException("binance: couldn't get symbol from exchange.symToInst. Websocket message: $msg")
		val type = streamNameSplit[1]
		when(type) {
			"trade" -> processTradeMessage(instrument, node.get("data"))
			"depth" -> processBookMessage(instrument, node.get("data"))
			else    -> println(msg)
		}
	}
	
	override suspend fun subscribe() {
	}
	
/*	{
		"e": "trade",     // Event type
		"E": 123456789,   // Event time
		"s": "BNBBTC",    // Symbol
		"t": 12345,       // Trade ID
		"p": "0.001",     // Price
		"q": "100",       // Quantity
		"b": 88,          // Buyer order ID
		"a": 50,          // Seller order ID
		"T": 123456785,   // Trade time
		"m": true,        // Is the buyer the market maker?
		"M": true         // Ignore
	}*/
	
	suspend fun processTradeMessage(instrument: Instrument, data: JsonNode) {
		val price = data.get("p").asDouble()
		val amount = data.get("q").asDouble()
		val side = if(data.get("m").asBoolean() == true) Side.SELL else Side.BUY
		val time = data.get("E").asLong()
		
		instrument.channel.send(Message(Trades(listOf(Trade(price = price,
		                                                    amount = amount,
		                                                    initiatingSide = side,
		                                                    time = time)))))
	}
	
	
/*	{
		"e": "depthUpdate", // Event type
		"E": 123456789,     // Event time
		"s": "BNBBTC",      // Symbol
		"U": 157,           // First update ID in event
		"u": 160,           // Final update ID in event
		"b": [              // Bids to be updated
		[
			"0.0024",       // Price level to be updated
			"10"            // Quantity
		]
		],
		"a": [              // Asks to be updated
		[
			"0.0026",       // Price level to be updated
			"100"           // Quantity
		]
		]
	}*/
	
	suspend fun processBookMessage(instrument: Instrument, data: JsonNode) {
		val time = data.get("E").asLong()
		// "u" is the last update for these operations, "U" is the first update
		// we need the last, if it is larger then the orderbook sequence, we process it
		// this is done in BookMessageHandler
		val sequence = data.get("u").asLong()
		val asks = data.get("a").map { arrayToBookOperation(Side.SELL, time, it) }
		val bids = data.get("b").map { arrayToBookOperation(Side.BUY, time, it) }
		
		
		instrument.channel.send(Message(content = BookOperations(ops = asks + bids,
		                                                         sequence = sequence),
		                                timestamp = time))
	}
	
	fun arrayToBookOperation(side: Side, time: Long, node: JsonNode): BookOperation {
		val price = node[0].asDouble()
		val amount = node[1].asDouble()
		val id = price.toString()
		val bookEntry = BookEntry(price, amount, id, time)
		
		return if (amount == 0.0) {
			BookOperation.Delete(side, bookEntry)
		} else {
			BookOperation.Insert(side, bookEntry)
		}
	}
	
	fun getBinanceStreamAddress(): String {
		val streamsList = mutableListOf<String>()
		Exchange.BinanceExchange.instruments.forEach {
			val sym = it.symbol.toLowerCase()
			streamsList.add("${sym}@trade")
			streamsList.add("${sym}@depth@100ms")
		}
		val address = "wss://stream.binance.com:9443/stream?streams=${streamsList.joinToString("/")}"
		return address
		// return "wss://stream.binance.com:9443/stream?streams=btcusdt@depth@100ms"
	}
}