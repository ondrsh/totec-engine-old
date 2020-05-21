package tothrosch.networking.rest.adapter.gdax

import org.apache.http.client.methods.HttpGet
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.networking.rest.RestClient

/**
 * Created by ndrsh on 02.07.17.
 */

object CoinbaseRestClient : RestClient(Exchange.CoinbaseExchange) {
	val baseAddress: String = "https://api.pro.coinbase.com"
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument) =
		HttpGet(baseAddress + "/products/${instrument.symbol}/book?level=3")
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("implement this")
	}
	
	override fun getActiveInstrumentsHttpRequest() = HttpGet(baseAddress + "/products")
}