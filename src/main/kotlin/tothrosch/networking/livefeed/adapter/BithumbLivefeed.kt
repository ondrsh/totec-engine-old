package tothrosch.networking.livefeed.adapter

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import tothrosch.engine.message.ConnectionState
import tothrosch.engine.message.Message
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.Currency
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.networking.livefeed.LiveFeed

@WebSocket(maxTextMessageSize = 65536 * 1000)
class BithumbLivefeed(exchange: Exchange) : LiveFeed(exchange) {

	// specified the rest intervals for requesting books (just one request for all books)
	// and trades (each pair individually)
	val bookInterval = 900L
	var lastBookRequestSent = 0L


	val tradeIntervals: HashMap<CurrencyPair, Long> = hashMapOf(
		CurrencyPair(Currency.BTC, Currency.KRW) to 1200L,
		CurrencyPair(Currency.BCH, Currency.KRW) to 1800L,
		CurrencyPair(Currency.DASH, Currency.KRW) to 3000L,
		CurrencyPair(Currency.ETH, Currency.KRW) to 3000L,
		CurrencyPair(Currency.ETC, Currency.KRW) to 4000L,
		CurrencyPair(Currency.LTC, Currency.KRW) to 6000L,
		CurrencyPair(Currency.QTUM, Currency.KRW) to 11_000L,
		CurrencyPair(Currency.XMR, Currency.KRW) to 4000L,
		CurrencyPair(Currency.ZEC, Currency.KRW) to 10_000L
	)
	val lastTradesSent: HashMap<CurrencyPair, Long> = hashMapOf()

/*	val bookPullerPullers: ArrayList<Puller.BookPuller> = arrayListOf()
	val tradePullers: ArrayList<Puller.TradesPuller> = arrayListOf()*/


	suspend override fun processMessage(msg: String) {

	}

	override fun rawConnect() {
		GlobalScope.launch(exchange.coroutineContext) {
			delay(5000)
			for (instrument in exchange.instruments) {
				// bookPullerPullers.add(Puller.BookPuller(instrument, bookInterval))
				// tradePullers.add(Puller.TradesPuller(instrument, tradeIntervals[instrument.pair]!!))
			}
		}

		runBlocking { exchange.instruments.forEach { it.channel.send(Message(ConnectionState.CONNECTED)) } }
	}

	override fun disconnect() {
	}

	override suspend fun subscribe() {
	}


}