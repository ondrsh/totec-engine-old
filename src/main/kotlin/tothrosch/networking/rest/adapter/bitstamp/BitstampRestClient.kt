package tothrosch.networking.rest.adapter.bitstamp

import org.apache.http.client.methods.HttpGet
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.networking.rest.RestClient

/**
 * Created by ndrsh on 02.07.17.
 */

object BitstampRestClient : RestClient(Exchange.BitstampExchange) {
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("implement this")
	}
	
	val baseAddress: String = "https://www.bitstamp.net/api/v2/"
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument) =
		HttpGet(baseAddress + "order_book/${instrument.symbol}/")
	
	override fun getActiveInstrumentsHttpRequest() = HttpGet(baseAddress + "trading-pairs-info/")
}