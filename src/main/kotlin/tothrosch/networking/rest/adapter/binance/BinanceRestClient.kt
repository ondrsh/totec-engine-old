package tothrosch.networking.rest.adapter.binance

import org.apache.http.client.methods.HttpGet
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.networking.rest.RestClient

/**
 * Created by ndrsh on 02.07.17.
 */

object BinanceRestClient : RestClient(Exchange.BinanceExchange) {
	val baseAddress: String = "https://api.binance.com"
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument) =
		HttpGet(baseAddress + "/api/v3/depth?symbol=${instrument.symbol}")
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("implement this")
	}
	
	override fun getActiveInstrumentsHttpRequest() = HttpGet(baseAddress + "/api/v3/ticker/24hr")
	
}