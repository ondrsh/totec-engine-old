package tothrosch.networking.livefeed.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
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
import tothrosch.networking.livefeed.LiveFeedException
import tothrosch.networking.livefeed.WebsocketClient
import tothrosch.util.logLive

/**
 * Created by ndrsh on 15.06.17.
 */


// five kinds of messages:
// 1) type: "lastMsgTime" - incoming lastMsgTime every few seconds. update lastMsgTime time and then ignore
// 2) type: "received"  - sent for all orders, means that gdax has received them, but does NOT mean they are in the orderbook, so just update lastMsgTime time and ignore then
// 3) type: "open"      - means that the order is open in the book now.
// 4) type: "done"      - means that the order is not in the book anymore. no messages with this order-id will


@WebSocket(maxTextMessageSize = 65536 * 1000)
object CoinbaseWebsocket : WebsocketClient(Exchange.CoinbaseExchange) {
	
	
	val mapper = ObjectMapper()
	override val address: String = "wss://ws-feed.pro.coinbase.com"
	
	override suspend fun subscribe() {
		val request = mapper.createObjectNode()
		request.put("type", "subscribe")
		request.replace("product_ids", getSubscriptionArray())
		request.replace("channels", mapper.createArrayNode().add("full"))
		sendMessage(request.toString())
	}
	
	
	override suspend fun processMessage(msg: String) {
		// println(msg)
		val node: JsonNode = mapper.readValue(msg)
		val nodeType: String = node.get("type")?.asText() ?: throw LiveFeedException("Gdax Dumbsocket - unknown action for message: $msg")
		
		if (isHeartBeatOrReceived(nodeType))
			return
		if (nodeType == "subscriptions")
			return
		
		val symbol: String = node.get("product_id")?.asText() ?: throw LiveFeedException("Gdax Dumbsocket - couldn't get sym for msg $msg")
		val sequence: Long = node.get("sequence")?.asLong() ?: throw LiveFeedException("Gdax Dumbsocket - couldn't get sequence for msg $msg")
		// val init: Initialization = booksInitialization.get(symbol) ?: throw LiveFeedException("Gdax Dumbsocket - couldn't get initialization for string $symbol")
		val instrument: Instrument = exchange.symToInst[symbol] ?: throw LiveFeedException("Gdax Dumbsocket - couldn't get instrument for string $symbol")
		
		processInstrumentMessage(nodeType, node, sequence, instrument)
		
	}
	
	
	private suspend fun processInstrumentMessage(action: String, node: JsonNode, sequence: Long, instrument: Instrument) {
		val side: Side = getSide(node.get("side").asText())
		
		when (action) {
			"open"   -> instrument.channel.send(
				Message(content = BookOperations(ops = listOf(BookOperation.Insert(side = side,
				                                                                   bookEntry = createBookEntry(node))),
				                                 sequence = sequence)))
			"done"   -> instrument.channel.send(
				Message(content = BookOperations(ops = listOf(BookOperation.Delete(side = side,
				                                                                   bookEntry = createBookEntry(node))),
				                                 sequence = sequence)))
			"match"  -> {
				val (tradeMsg, changeMsg) = processMatch(node, sequence, instrument)
				instrument.channel.send(tradeMsg as Message<Any>)
				instrument.channel.send(changeMsg as Message<Any>)
			}
			"change" -> {
				val amount = node.get("new_size")?.asDouble()
				amount?.let {
					val deltaEntry = BookEntry(0.0, amount, node.get("order_id").asText())
					instrument.channel.send(
						Message(content = BookOperations(ops = listOf(BookOperation.Change(side = side,
						                                                                   deltaEntry = deltaEntry)),
						                                 sequence = sequence)))
				}
				// we check if new_size exists at "change" because of:
				// {"type":"change","side":"buy","old_funds":"228.5120900618000000","new_funds":"61.7659102493000000","order_id":"98c96dd1-3fd3-4fe0-a58f-d354b43f66be","product_id":"LTC-USD","sequence":194891838,"time":"2017-06-21T09:38:41.098000Z"}
			}
			else     -> {
				logLive("oh my god")
				throw LiveFeedException("GdaxDumbsocket error - trying to process book operation, but action has value $action - don't know what that is")
			}
		}
	}
	
	// processes a trade and then the change of the maker_id
	private fun processMatch(node: JsonNode, sequence: Long, instrument: Instrument): Pair<Message<Trades>, Message<BookOperations>> {
		val side: Side = getSide(node.get("side").asText())
		val trade = Trade(price = node.get("price").asDouble(),
		                  amount = node.get("size").asDouble(),
		                  initiatingSide = if (side == Side.SELL) Side.BUY else Side.SELL)
		
		instrument
		
		
		
		val tradeMsg = Message(Trades(listOf(trade), sequence))
		
		val bookChange = BookOperation.Change(
			side = side,
			deltaEntry = BookEntry(
				price = node.get("price").asDouble(),
				amount = -1 * node.get("size").asDouble(),
				id = node.get("maker_order_id").asText()
			)
		)
		
		
		return (tradeMsg to Message(BookOperations(listOf(bookChange), sequence)))
	}
	
	private fun createBookEntry(node: JsonNode): BookEntry {
		return BookEntry(node.get("price")?.asDouble() ?: 0.0, node.get("remaining_size")?.asDouble() ?: 0.0, node.get("order_id").asText())
	}
	
	
	fun getSubscriptionArray(): ArrayNode {
		val subArray = mapper.createArrayNode()
		
		for (symbol: String in exchange.symToInst.keys) {
			subArray.add(symbol)
		}
		
		return subArray
	}
	
	fun getSide(sideAsString: String): Side =
		when (sideAsString) {
			"buy"  -> Side.BUY
			"sell" -> Side.SELL
			else   -> throw LiveFeedException("Gdax networking error - couldn't recognize side $sideAsString")
		}
	
	fun isHeartBeatOrReceived(action: String): Boolean {
		return action == "lastMsgTime" || action == "received"
	}
	
	
	/* {"type":"done","side":"buy","order_id":"d3b2745b-4a28-4641-b520-6418034e85e0","reason":"canceled","product_id":"BTC-USD","price":"2452.32000000","remaining_size":"0.06501971","bookSequence":3341038600,"time":"2017-06-16T22:45:43.086000Z"}
	{"type":"received","order_id":"9e26bcbd-f0f3-42a2-befd-58cdfb35ee33","order_type":"limit","size":"0.10000000","price":"2455.81000000","side":"sell","client_oid":"f538039b-c9e6-4805-94cc-4ecc07b45e89","product_id":"BTC-USD","bookSequence":3341038601,"time":"2017-06-16T22:45:43.092000Z"}
	{"type":"match","trade_id":16924414,"maker_order_id":"1f47bfdb-e5bf-40e8-90a6-11aeedd48ddb","taker_order_id":"7d6ec9bf-3a0d-4ca7-aae6-75af716004d6","side":"sell","size":"0.10000000","price":"2461.00000000","product_id":"BTC-USD","bookSequence":3341983107,"time":"2017-06-17T00:51:54.540000Z"}
	{"type":"received","order_id":"b32eba4f-bd6e-4587-b7b6-3e6a756a70d4","order_type":"limit","size":"0.10000000","price":"2455.81000000","side":"sell","client_oid":"0392c8ae-16e3-459c-9831-a8914b758d52","product_id":"BTC-USD","bookSequence":3341038608,"time":"2017-06-16T22:45:43.139000Z"}
	{"type":"open","side":"sell","price":"2455.81000000","order_id":"b32eba4f-bd6e-4587-b7b6-3e6a756a70d4","remaining_size":"0.10000000","product_id":"BTC-USD","bookSequence":3341038609,"time":"2017-06-16T22:45:43.139000Z"}
	{"type":"received","order_id":"503637c7-d3a4-45e7-93c6-9d9639b81b75","order_type":"limit","size":"0.08423700","price":"2455.33000000","side":"sell","client_oid":"5ba85e8c-e438-43f8-be3c-9b1fe57371a4","product_id":"BTC-USD","bookSequence":3341038610,"time":"2017-06-16T22:45:43.144000Z"}
	{"type":"open","side":"sell","price":"2455.33000000","order_id":"503637c7-d3a4-45e7-93c6-9d9639b81b75","remaining_size":"0.08423700","product_id":"BTC-USD","bookSequence":3341038611,"time":"2017-06-16T22:45:43.144000Z"}
	{"type":"lastMsgTime","last_trade_id":876416,"product_id":"ETH-BTC","bookSequence":317297395,"time":"2017-06-16T22:45:43.172000Z"}
	*/
	
}

