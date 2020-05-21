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
import tothrosch.networking.livefeed.LiveFeedException
import tothrosch.networking.livefeed.WebsocketClient
import java.util.*
import kotlin.collections.HashMap

@WebSocket(maxTextMessageSize = 65536 * 1000)
object KrakenWebsocket : WebsocketClient(Exchange.KrakenExchange) {
	
	override val address: String = "wss://ws.kraken.com"
	val mapper = ObjectMapper()
	val CHANNELS: HashMap<Int, KrakenChannel> = hashMapOf()
	val snapshotSent = mutableSetOf<Instrument>()
	class KrakenChannel(val instrument: Instrument, val isBookChannel: Boolean)
	
	
	override suspend fun subscribe() {
		CHANNELS.clear()
		snapshotSent.clear()
		val bookSubscription = mapper.createObjectNode()
		bookSubscription.put("name", "book")
		bookSubscription.put("depth", 500)
		
		val tradeSubscription = mapper.createObjectNode()
		tradeSubscription.put("name", "trade")
		
		subscribeTo(bookSubscription)
		subscribeTo(tradeSubscription)
	}
	
	
	fun subscribeTo(subscription: ObjectNode) {
		val request = mapper.createObjectNode()
		val pairArray = mapper.createArrayNode()
		exchange.instruments.forEach { pairArray.add(it.symbol) }
		request.put("event", "subscribe")
		request.replace("pair", pairArray)
		request.replace("subscription", subscription)
		sendMessage(request.toString())
	}
	
	override suspend fun processMessage(msg: String) {
		// println(msg)
		val node: JsonNode = mapper.readValue(msg)
		if (node !is ArrayNode && node.get("event").asText() == "subscriptionStatus" && node.get("status").asText() == "subscribed") {
			val channelId = node.get("channelID").asInt()
			val instrument = exchange.symToInst.get(node.get("pair").asText()) ?: throw LiveFeedException("kraken websocket: couldn't get pair ${node.get("pair")} from symToInst")
			val channelName = node.get("channelName").asText()
			val isBookChannel =
				if (channelName.startsWith("book")) true
				else if (channelName == "trade")  false
				else throw RuntimeException("kraken websocket: couldn't determine MessageHandler")
			CHANNELS.put(channelId, KrakenChannel(instrument, isBookChannel))
			return
		}
		if (node is ArrayNode == false) {
			return
		}
		val channel = CHANNELS[node[0].asInt()] ?: throw LiveFeedException("kraken websocket: couldn't get channel for message $msg")
		if (channel.isBookChannel) {
			if (snapshotSent.contains(channel.instrument)) {
				processBookOperationsMessage(node, channel)
			} else processBookSnapshotMessage(node, channel)
		} else processTradeMessage(node, channel)
		
	}
	
	private suspend fun processBookSnapshotMessage(array: ArrayNode, krakenChannel: KrakenChannel) {
		val node = array[1]
		val asksMap = HashMap(node.get("as").associate {
			val bookEntry = it.toBookEntry()
			bookEntry.id to bookEntry
		})
		val bidsMap = HashMap(node.get("bs").associate {
			val bookEntry = it.toBookEntry()
			bookEntry.id to bookEntry
		})
		val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
		val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
		val book = Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet))
		krakenChannel.instrument.channel.send(Message(book))
		snapshotSent.add(krakenChannel.instrument)
	}
	
	// [480,{"a":[["0.29734000","29005.03783135","1572291631.840498"]]},{"b":[["0.29734000","0.00000000","1572291631.841016"]]},"book-500","XRP/USD"]
	// care, asks and bids are in different nodes
	private suspend fun processBookOperationsMessage(array: ArrayNode, krakenChannel: KrakenChannel) {
		val bookOperationList = mutableListOf<BookOperation>()
		for (i in 1..array.size() - 1) {
			val node = array[i]
			val asks = node["a"]?.map {
				val bookEntry = it.toBookEntry()
				if (bookEntry.amount == 0.0) BookOperation.Delete(Side.SELL, bookEntry)
				else BookOperation.Insert(Side.SELL, bookEntry)
			}
			val bids = node["b"]?.map {
				val bookEntry = it.toBookEntry()
				if (bookEntry.amount == 0.0) BookOperation.Delete(Side.BUY, bookEntry)
				else BookOperation.Insert(Side.BUY, bookEntry)
			}
			bids?.let { bookOperationList.addAll(it) }
			asks?.let { bookOperationList.addAll(it) }
		}
		krakenChannel.instrument.channel.send(Message(BookOperations(bookOperationList)))
	}
	
	private fun JsonNode.toBookEntry(): BookEntry {
		val price = this[0].asDouble()
		val amount = this[1].asDouble()
		val id = price.toString()
		val time = (this[2].asDouble() * 1000).toLong()
		return BookEntry(price, amount, id, time)
	}
	
	private suspend fun processTradeMessage(array: ArrayNode, krakenChannel: KrakenChannel) {
		val tradesArray = array[1]
		val trades = tradesArray.map { it.toTrade() }
		krakenChannel.instrument.channel.send(Message(Trades(trades)))
	}
	
	private fun JsonNode.toTrade() = Trade(price= this[0].asDouble(),
	                                       amount = this[1].asDouble(),
	                                       initiatingSide = if (this[3].asText() == "s") Side.BUY else Side.SELL,
	                                       time = this[2].asLong())
		
	
}