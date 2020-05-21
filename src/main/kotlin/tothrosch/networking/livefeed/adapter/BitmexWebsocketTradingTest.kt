/*
package tothrosch.trading.networking.livefeed.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.experimental.delay
import tothrosch.trading.engine.position.bitmex.BitmexPosition
import tothrosch.trading.exchange.Exchange
import tothrosch.trading.exchange.currencies.CurrencyPair
import tothrosch.trading.instrument.Instrument
import tothrosch.trading.json.bitmex.CurrencyPairDeserializer
import tothrosch.trading.networking.rest.adapter.bitmex.BitmexRestClient


@javax.websocket.ClientEndpoint
class BitmexWebsocketTradingTest(exchange: Exchange) : BitmexWebsocket(exchange) {


	init {
		val module = SimpleModule()
		module.addDeserializer(CurrencyPair::class.java, CurrencyPairDeserializer(exchange.symToInst))
		mapper.registerModule(module)
	}


	override suspend fun processMessage(msg: String) {
		println(msg)
		val node: JsonNode = mapper.readTree(msg)
		when (node.get("table")?.asText()) {
			"instrument"        -> processInstrumentMessage(node)
			"order"             -> processOrderMessage(node)
			"position"          -> processPositionsMessage(node)
		}
	}

	// 1270539918001

	override suspend fun subscribe() {
		authenticate()
		delay(500)
		val request = mapper.createObjectNode()
		request.put("op", "subscribe")
		request.set("args", getIndividualSubscriptionArray(exchange.globalInstruments))

		sendMessage(request.toString())
		println("sent subscription: $request")
		//sendMessage("""{"op": "subscribe", "args": ["orderBookL2:XBTUSD"]}""")
	}


	override fun getIndividualSubscriptionArray(globalInstruments: List<Instrument>): ArrayNode {
		val subArray = mapper.createArrayNode()

		for (symbol: String in exchange.instToSym.filter { globalInstruments.contains(it.key) }.values) {
			subArray.add("instrument:$symbol")
			subArray.add("order:$symbol")
			//subArray.add("orderBookL2:$symbol")

		}
		//subArray.add("margin")
		// subArray.add("settlement:$symbol")
		subArray.add("position")



		// subArray.add("insurance")
		return subArray
	}

	// {"op": "authKey", "args": ["<APIKey>", <nonce>, "<signature>"]}
	fun authenticate() {
		val authenticationNode = mapper.createObjectNode()
		authenticationNode.put("op", "authKey")

		val nonce = exchange.nonce.incrementAndGet()
		val argNode = mapper.createArrayNode()
		argNode.add(exchange.restClient.apiID)
		argNode.add(nonce)
		argNode.add((exchange.restClient as BitmexRestClient).getSignature("GET", "/realtime", nonce))

		authenticationNode.set("args", argNode)
		sendMessage(authenticationNode.toString())
		println("sent authentication node: $authenticationNode")
	}




}
*/
