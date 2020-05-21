package tothrosch.networking.rest.adapter.gemini

import org.apache.http.client.methods.HttpGet
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.networking.rest.RestClient
import tothrosch.settings.Settings

object GeminiRestClient : RestClient(Exchange.GeminiExchange) {
	val baseAddress: String = "https://api.gemini.com/v1"
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument) =
		HttpGet(baseAddress + "/book/${instrument.symbol}?limit_bids=${Settings.bookArraySize}&limit_asks=${Settings.bookArraySize}")
	
	
	override fun getActiveInstrumentsHttpRequest() = HttpGet(baseAddress + "/symbols")
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("implement this")
	}
	
}