package tothrosch.networking.rest

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tothrosch.engine.message.Message
import tothrosch.engine.message.Request
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Book
import tothrosch.util.time.TimeScope


sealed class Puller(val instrument: Instrument, val interval: Long) : TimeScope by instrument {
	var lastAnswer: Long = 0L

	
/*
	class BookPuller(instrument: Instrument, interval: Long) : Puller(instrument, interval) {
		val bookPullerChannel: Channel<Message<Book>> = Channel(1)
		val req = Request<Book>(returnChannel = bookPullerChannel,
		                        base = Book(),
		                        created = System.currentTimeMillis())
		val restClient = instrument.exchange.restClient

		init {
			GlobalScope.launch(instrument.exchange.threadContext) {
				while (isActive) {
					if (lastAnswer.ago < interval) {
						delay(Math.max(interval - lastAnswer.ago, 1L))
					}

					restClient.sendRequest(http = restClient.getBookSnapshotHttpRequest(instrument),
					                       request = req)
					//println("waiting for receive")
					val bookMessage = bookPullerChannel.receive()
					//println("received, sending to instrument")
					instrument.bookMessageHandler.send(bookMessage)

					if (bookMessage.error != null) {
						println("${bookMessage.error}")
						delay(20_000)
					} else {
						lastAnswer = now
					}
				}
			}
		}
	}

	class TradesPuller(instrument: Instrument, interval: Long) : Puller(instrument, interval) {
		val returnChannel: Channel<Message<tothrosch.instrument.Trades>> = Channel(1)
		val req = Request.Rest(returnChannel, instrument)

		init {
			GlobalScope.launch(instrument.exchange.threadContext) {
				while (isActive) {
					if (lastAnswer.ago < interval) {
						delay(Math.max(interval - lastAnswer.ago, 1L))
					}

					instrument.exchange.restClient.tradeRequestHandler.handleRequest(req)
					val tradesMessage = returnChannel.receive()
					instrument.tradeMessageHandler.send(tradesMessage)

					if (tradesMessage.error != null) {
						println("${tradesMessage.error}")
						delay(20_000)
					} else {
						lastAnswer = now
					}
				}
			}
		}
	}*/
}
