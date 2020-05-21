package tothrosch.networking.rest.adapter.bitfinex

import org.apache.http.client.methods.HttpGet
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.networking.rest.RestClient
import tothrosch.settings.Settings

/**
 * Created by ndrsh on 07.07.17.
 */

object BitfinexRestClient : RestClient(Exchange.BitfinexExchange) {
	val baseAddress: String = "https://api.bitfinex.com/v1/"
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument) =
		HttpGet(baseAddress + "book/${instrument.symbol}?limit_bids=${Settings.bookArraySize}&limit_asks=${Settings.bookArraySize}")
	
	override fun getActiveInstrumentsHttpRequest() = HttpGet(baseAddress + "tickers")
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("implement this")
	}
}