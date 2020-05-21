package tothrosch.networking.livefeed.adapter

import tothrosch.exchange.Exchange
import tothrosch.networking.livefeed.LiveFeed

object GeminiLivefeed : LiveFeed(Exchange.GeminiExchange) {

	private val singleWebsockets: MutableList<GeminiSingleWebsocket> = arrayListOf()

	suspend override fun processMessage(msg: String) {
		// single websockets are doing this and sending messages to instrument themselves
	}

	override fun rawConnect() {
		if (singleWebsockets.size == 0) {
			exchange.instruments.forEach { singleWebsockets.add(GeminiSingleWebsocket(it, this)) }
		}

		singleWebsockets.forEach { it.rawConnect() }
	}

	override fun disconnect() = singleWebsockets.forEach { it.disconnect() }


	override suspend fun subscribe() {
		// subscribing does not exist - you just open the channel and the fun starts
	}
}