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
import tothrosch.instrument.book.*
import tothrosch.networking.livefeed.LiveFeedException
import tothrosch.networking.livefeed.WebsocketClient
import java.util.*
import kotlin.collections.HashMap

// we subscribe to "order_book_$sym" or "live_trades_$sym"
// so channels for Bitcoin/USD are either "order_book_btcusd" or "live_trades_btcusd"

@WebSocket(maxTextMessageSize = 65536 * 1000)
object BitstampWebsocket : WebsocketClient(Exchange.BitstampExchange) {
	
	override val address: String by lazy { "wss://ws.bitstamp.net" }
	val mapper = jacksonObjectMapper()
	
	/*
		{
		"event": "bts:subscribe",
		"data": {
			"channel": "[channel_name]"
		}
		}
	*/
	
	override suspend fun subscribe() {
		for (sym: String in exchange.instruments.map { it.symbol }) {
			subscribeBook(sym)
			subscribeTrades(sym)
		}
	}
	
	fun subscribeBook(sym: String) {
		val request = mapper.createObjectNode()
		request.put("event", "bts:subscribe")
		request.with("data").put("channel", "order_book_$sym")
		sendMessage(request.toString())
	}
	
	fun subscribeTrades(sym: String) {
		val request = mapper.createObjectNode()
		request.put("event", "bts:subscribe")
		request.with("data").put("channel", "live_trades_$sym")
		sendMessage(request.toString())
		sendMessage(request.toString())
	}
	
	override suspend fun processMessage(msg: String) {
		val node: JsonNode = mapper.readValue(msg)
		val event = node.get("event").asText()
		val channelSplit = node.get("channel").asText().split("_")
		val instrument = exchange.symToInst.get(channelSplit.last()) ?: throw
		LiveFeedException("Couldn't get channel from bitstamp websocket, node: ${node.toPrettyString()}")
		
		when (event) {
			"data" -> processBook(instrument, node.get("data"))
			"trade" -> { processTrades(instrument, node.get("data")) }
		}
		// channels are either "order_book_btcusd" or "live_trades_btcusd" so we just look
		// at the first element of the splitString
	}
	
	private suspend fun processBook(instrument: Instrument, data: JsonNode) {
		val sequence = data.get("microtimestamp").asLong()
		
		fun arrayToMap(jsonArray: JsonNode, time: Long, side: Side): HashMap<String, BookEntry> {
			return HashMap(jsonArray.associate {
				val entry = BookEntry(price = it[0].asDouble(),
				                      amount = it[1].asDouble(),
				                      id = it[0].asText(),
				                      time = time)
				entry.id to entry
			})
		}
		
		val bidsMap = arrayToMap(jsonArray = data.get("bids"),
		                         time = sequence / 1000,
		                         side = Side.BUY)
		
		val asksMap = arrayToMap(jsonArray = data.get("asks"),
		                         time = sequence / 1000,
		                         side = Side.SELL)
		
		val bidsSet = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
		val asksSet = TreeSet(asksMap.values.toSortedSet(AsksComparator))
		val book = Book(bidsMutable = BidsMutable(bidsMap, bidsSet),
		                asksMutable = AsksMutable(asksMap, asksSet),
		                sequence = sequence)
		instrument.channel.send(Message(book))
	}
	
	suspend fun processTrades(instrument: Instrument, data: JsonNode) {
		val price = data.get("price").asDouble()
		val amount = data.get("amount").asDouble()
		// type == 0 means Buy, type == 1 means sell
		val side = if (data.get("type").asInt() == 0) Side.BUY else Side.SELL
		val sequence = data.get("microtimestamp").asLong()
		val time = sequence / 1000
		val trade = Trade(price = price,
		                  amount = amount,
		                  initiatingSide = side,
		                  time = time)
		instrument.channel.send(Message(Trades(listOf(trade), sequence)))
	}
	
	}