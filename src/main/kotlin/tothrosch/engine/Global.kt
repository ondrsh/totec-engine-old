package tothrosch.engine

import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.Currency
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.Instrument
import tothrosch.instrument.Mode

// this whole class is not thread-safe, so take care!
object Global {
	var mode: Mode = Mode.START
	val startPairs: MutableSet<CurrencyPair> = mutableSetOf(CurrencyPair(Currency.BTC, Currency.USD))
	val startCurrencies: MutableSet<Currency> = mutableSetOf()
	
	val exchanges: Set<Exchange> = Exchange::class.sealedSubclasses.map { it.objectInstance!! }.filter { it !is Exchange.GeminiExchange }.toSet()
	val instruments = mutableSetOf<Instrument>()
	val tradingInstrument by lazy {
		println("does this get initialized when we just type tradingInstrument?")
		instruments.find {
			it.exchange == Exchange.BitmexExchange
					&& it.symbol == "XBTUSD"
		}!!
	}
}
