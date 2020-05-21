package tothrosch.networking.livefeed.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.client.WebSocketClient
import tothrosch.engine.message.ConnectionState
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
import tothrosch.settings.Settings
import tothrosch.util.logLive
import java.net.ConnectException
import java.util.*
import java.util.concurrent.CompletionStage
import kotlin.collections.HashMap

@WebSocket(maxTextMessageSize = 65536 * 1000)
class GeminiSingleWebsocket(val instrument: Instrument, val livefeed: GeminiLivefeed) {
	
	
	val address: String = "wss://api.gemini.com/v1/marketdata/${instrument.pair.base}${instrument.pair.quote}?lastMsgTime=true"
	private lateinit var client: WebSocketClient
	private lateinit var session: Session
	val mapper: ObjectMapper = ObjectMapper()
	@Volatile var lastFullChannelMsg: Long = System.currentTimeMillis()
	
	private val livefeedJob: SendChannel<String> = GlobalScope.actor(Settings.appContext, 1000) {
		try {
			while (isActive) {
				for (msg in channel) {
					processMessage(msg)
				}
			}
		} catch (ex: Exception) {
			logLive("GEMINI ${instrument.pair} livefeedJob broke down --> ")
			ex.printStackTrace()
		}
	}
	
	
	fun rawConnect() {
		if (::client.isInitialized == false) {
			client = WebSocketClient()
			client.httpClient.connectTimeout = 5000
		}
		client.start()
		@Suppress("BlockingMethodInNonBlockingContext", "UNCHECKED_CAST")
		client.connect(this, java.net.URI.create(address)) as CompletionStage<Session>
	}
	
	fun disconnect() {
		try {
			session.close()
		} catch (ex: Exception) {
			logLive("GEMINI ${instrument.pair} failed to close session - probably was closed already")
		}
	}
	
	
	suspend fun processMessage(msg: String) {
		val node: JsonNode = mapper.readValue(msg)
		if (node.get("type").asText() == "lastMsgTime") return
		val sequence: Long = node.get("eventId").asLong()
		val eventNodes: JsonNode = node.get("events")
		if (eventNodes.get(0).get("type").asText() == "change" && eventNodes.get(0).get("reason").asText() == "initial") {
			processFullBook(eventNodes, sequence)
		} else {
			val tradeJsons: List<JsonNode> = eventNodes.filter { it.get("type").asText() == "trade" }
			val bookJsons: List<JsonNode> = eventNodes.filter { it.get("type").asText() == "change" }
			if (tradeJsons.isNotEmpty()) processTrades(tradeJsons, sequence)
			if (bookJsons.isNotEmpty()) processBookOperations(bookJsons, sequence)
		}
		// log(now, msg)
	}
	
	private suspend fun processTrades(tradeJsons: List<JsonNode>, sequence: Long) {
		val trades: List<Trade> = tradeJsons.mapNotNull { createTradeEntry(it) }
		if (trades.isNotEmpty()) instrument.channel.send(Message(Trades(trades = trades, sequence = sequence)))
	}
	
	private fun createTradeEntry(node: JsonNode): Trade? {
		val side: Side = when (node.get("makerSide").asText()) {
			"ask"     -> Side.BUY
			"bid"     -> Side.SELL
			"auction" -> return null
			else      -> throw LiveFeedException("could not get trade side from gemini websocket, node $node")
		}
		val price: Double = node.get("price").asDouble()
		val amount: Double = node.get("amount").asDouble()
		
		return Trade(price = price, amount = amount, initiatingSide = side)
	}
	
	suspend fun processBookOperations(bookJsons: List<JsonNode>, sequence: Long) {
		val bookOps: List<BookOperation> = bookJsons.map { createBookOperation(it) }
		instrument.channel.send(Message(BookOperations(bookOps, sequence)))
	}
	
	suspend fun createBookOperation(node: JsonNode): BookOperation {
		val side: Side = when (node.get("side").asText()) {
			"bid" -> Side.BUY
			"ask" -> Side.SELL
			else  -> throw LiveFeedException("could not get bookentry side from gemini node $node")
		}
		val entry: BookEntry = createBookEntry(node)
		return when (entry.amount) {
			0.0  -> BookOperation.Delete(side, entry)
			else -> BookOperation.Insert(side, entry)
		}
	}
	
	suspend fun processFullBook(eventNodes: JsonNode, sequence: Long) {
		
		val dataBySide: Map<String, List<JsonNode>> = eventNodes.groupBy { it.get("side").asText() }
		
		// only reverse bidmap, because both are sorted by price.... dumb gemini
		val bidsMap = HashMap<String, BookEntry>(dataBySide["bid"]
			                                         ?.reversed()
			                                         ?.take(Settings.bookArraySize)
			                                         ?.associate { it.get("price").asText() to createBookEntry(it) })
		
		
		val asksMap = HashMap<String, BookEntry>(dataBySide["ask"]
			                                         ?.take(Settings.bookArraySize)
			                                         ?.associate { it.get("price").asText() to createBookEntry(it) })
		
		
		val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
		val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
		
		val book = Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet))
		instrument.channel.send(Message(book))
	}
	
	suspend fun createBookEntry(node: JsonNode): BookEntry {
		val price: Double = node.get("price").asDouble()
		val id: String = price.toString()
		val amount: Double = node.get("remaining").asDouble()
		return BookEntry(price, amount, id)
	}
	
	
	//fun sendMessage(message: String) = session?.asyncRemote?.sendText(message)
	
	
	@OnWebSocketConnect
	fun onConnect(session: Session) {
		this.session = session
		runBlocking { Exchange.GeminiExchange.instruments.forEach { it.channel.send(Message(ConnectionState.CONNECTED)) } }
	}
	
	
	@OnWebSocketClose
	fun onClose(session: Session, closeCode: Int, closeReason: String) {
		println("websocket on ${Exchange.GeminiExchange.name} closed, reason is $closeReason, code is $closeCode")
		this.session = session
		disconnect()
		runBlocking { Exchange.GeminiExchange.instruments.forEach { it.channel.send(Message(ConnectionState.DISCONNECTED)) } }
	}
	
	@OnWebSocketMessage
	fun onMessage(message: String) = runBlocking {
		if (livefeedJob.isClosedForSend) {
			logLive("channel is closed")
		}
		val hasCapacity = livefeedJob.offer(message)
		if (hasCapacity == false && (System.currentTimeMillis() - lastFullChannelMsg) > 10_000) {
			logLive("${Exchange.GeminiExchange.name} channel is full")
			lastFullChannelMsg = System.currentTimeMillis()
		}
	}
	
	@OnWebSocketError
	fun onError(session: Session, thr: Throwable) {
		logLive("${Exchange.GeminiExchange.name} websocket onError triggered. Error: ${thr?.message ?: "unknown error"}")
		thr.printStackTrace()
		if (thr is ConnectException) {
			// Attempt to reconnect
			livefeed.reconnectingChannel.offer(null)
		} else {
			// Ignore upgrade exception
		}
		session.close()
	}
}