package tothrosch.networking.rest.adapter.kraken

import org.apache.http.client.methods.HttpGet
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.networking.rest.RestClient


object KrakenRestClient : RestClient(Exchange.KrakenExchange) {
	
	override fun getActiveInstrumentsHttpRequest(): HttpGet  = HttpGet("https://api.kraken.com/0/public/AssetPairs")
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument): HttpGet = HttpGet("https://api.kraken.com/0/public/Depth?pair=${instrument.symbol}")
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}