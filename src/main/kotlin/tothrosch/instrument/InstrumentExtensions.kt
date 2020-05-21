package tothrosch.instrument

import org.apache.http.client.methods.HttpRequestBase

fun Instrument.getBookRequest(): HttpRequestBase {
	return this.exchange.restClient.getBookSnapshotHttpRequest(this)
}

fun Instrument.getTradesRequest(): HttpRequestBase {
	return this.exchange.restClient.getTradesHttpRequest(this)
}

