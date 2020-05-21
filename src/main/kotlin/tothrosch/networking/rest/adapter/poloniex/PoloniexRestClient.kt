/*package tothrosch.networking.rest.adapter.poloniex

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import tothrosch.engine.message.Error
import tothrosch.engine.message.Message
import tothrosch.engine.message.Request
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Book
import tothrosch.networking.ConnectionType
import tothrosch.networking.rest.RestClient
import tothrosch.settings.Settings
import tothrosch.util.time.getAgo
import tothrosch.util.isError

*//**
 * Created by ndrsh on 13.07.17.
 *//*

object PoloniexRestClient : RestClient(Exchange.PoloniexExchange) {
	
	val baseAddress: String = "https://poloniex.com"
	var bookMap: PoloniexBookMap = PoloniexBookMap()
	override val restBookDelay: Long = 0
	
	override fun getFromBookMap(request: Request.Rest<Book>): Message<Book> {
		val bookSnapshot: Book?
		if (bookMap.lastReceived.ago < Settings.restBookInterval) {
			bookSnapshot = bookMap.getBookSnapshot(mapper, request.instrument)
			if (bookSnapshot != null) return Message(bookSnapshot, type = ConnectionType.REST)
		}
		val response: HttpResponse = client.execute(HttpGet(baseAddress + "/public?command=returnOrderBook&currencyPair=all&depth=${Settings.bookArraySize}"))
		if (response.statusLine.statusCode.isError) return Message(null, Error.Request(response.statusLine.statusCode, "couldn't retrieve big poloniex orderbook", request), type = ConnectionType.REST)
		bookMap.responseToMap(exchange, mapper, response)
		val newBook: Book? = bookMap.getBookSnapshot(mapper, request.instrument)
		return if (newBook != null) {
			Message(newBook, type = ConnectionType.REST)
		} else {
			Message(null, Error.Request(response.statusLine.statusCode, "couldn't get single polniex orderbook from PoloniexBookMap", request), type = ConnectionType.REST)
		}
	}
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument): HttpGet {
		*//* val sym: String = exchange.instToSym[instrument]!!
		 return HttpGet(baseAddress + "public?command=returnOrderBook&currencyPair=$sym&depth=${Settings.bookArraySize}")*//*
		throw RuntimeException("error - function getBookSnapshotHttpRequest from PoloniexRestClient should have never been called!!!")
	}
	
	
	*//*fun getSymbols(): SupportedPairs {
		val httpGet = HttpGet(baseAddress + "public?command=returnTicker")
		val response: HttpResponse = client.execute(httpGet)
		val statusCode: Int = response.statusLine.statusCode
		if(statusCode.isError) throw RestClientException("could not get supported pairs from polo")
		try {
			val pairs = mapper.readValue(response.entity.content, SupportedPairs::class.java)
			return pairs
		}
		catch(ex: Exception)  {
			throw RestClientException("could not get supported pairs from polo")
		}
	}*//*
	
	override fun getTradesHttpRequest(instrument: Instrument): HttpGet {
		TODO("implement this")
	}
	
	
	override fun getActiveInstruments() = HttpGet(baseAddress + "/public?command=returnTicker")
	
}*/
