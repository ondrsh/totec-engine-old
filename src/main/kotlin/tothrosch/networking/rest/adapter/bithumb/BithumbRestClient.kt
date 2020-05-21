package tothrosch.networking.rest.adapter.bithumb

import org.apache.http.client.methods.HttpGet
import tothrosch.engine.message.Message
import tothrosch.engine.message.Request
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Book
import tothrosch.networking.rest.RestClient


/*
class BithumbRestClient(exchange: Exchange) : RestClient(exchange) {
	val baseAddress = "https://api.bithumb.com/"
	var bookMap: BithumbBookMap = BithumbBookMap()
	override val restBookDelay: Long = 0
	
	override fun getBookSnapshotHttpRequest(instrument: Instrument) =
		HttpGet(baseAddress + "public/orderbook/${instrument.symbol}?count=50")
	
	override fun getTradesHttpRequest(instrument: Instrument) =
		HttpGet(baseAddress + "public/recent_transactions/${instrument.symbol}?count=100")
	
	override fun getFromBookMap(request: Request.Rest<Book>): Message<Book> {
		val bookSnapshot: Book?
		throw RuntimeException(" NICHT IMPLEMENTIERT aber scheiss auf den exchange")
	*/
/*	if (bookMap.lastReceived.ago < Settings.restBookInterval) {
			bookSnapshot = bookMap.getBookSnapshot(mapper, request.instrument)
			if (bookSnapshot != null) return Message(bookSnapshot, type = ConnectionType.REST)
		}
		
		// println("executing BOOK request for pair ${request.instrument.pair}")
		val response: HttpResponse = client.execute(HttpGet(baseAddress + "public/orderbook/ALL?count=50"))
		if (response.statusLine.statusCode.isError) return Message<Book>(
			null,
			Error.Request(
				response.statusLine.statusCode,
				"couldn't retrieve big bithumb orderbook",
				request
			),
			type = ConnectionType.REST
		)
		bookMap.responseToMap(exchange, mapper, response)
		val newBook: Book? = bookMap.getBookSnapshot(mapper, request.instrument)
		return if (newBook != null) {
			Message(newBook, type = ConnectionType.REST)
		} else {
			Message(
				null,
				Error.Request(
					response.statusLine.statusCode,
					"couldn't get single bithumb orderbook from BithumbBookMap",
					request
				),
				type = ConnectionType.REST
			)
		}*//*

	}
	
	override fun getActiveInstruments(): HttpGet {
		TODO("implement this")
	}
	
	
}*/
