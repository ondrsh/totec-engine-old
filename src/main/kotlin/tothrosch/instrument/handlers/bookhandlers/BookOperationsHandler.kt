package tothrosch.instrument.handlers.bookhandlers

import tothrosch.engine.message.Message
import tothrosch.engine.message.Request
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Book
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.util.time.log.Event

class BookOperationsHandler(instrument: Instrument) : BookableHandler<BookOperations>(instrument) {
	
	override fun getBookOperationsMsg(msg: Message<BookOperations>): Message<BookOperations>? {
		// handle possible Initialization
		if (instrument.bookInit != null) {
			if (instrument.bookInit.initialized == false) {
				@Suppress("UNCHECKED_CAST")
				instrument.bookInit.msgList.add(msg)
				if (instrument.bookInit.sending == false) {
					val restClient = instrument.exchange.restClient
					restClient.executeRequest(http = restClient.getBookSnapshotHttpRequest(instrument),
					                          request = Request(returnChannel = instrument.channel,
					                                            content = Book(),
					                                            timeStamp = now))
					instrument.bookInit.sending = true
				}
				return null
			} else {
			// if initialized, but sequence number lower than orderbook, ignore message
				if (msg.content.sequence <= instrument.bookInit.snapshotSequence) {
					return null
				}
			}
		}
		instrument.book.processBookOperations(msg.content)?.let {
			msg.content = it
			// msg.addEvent(Event.BOOKOPS_PROCESSED)
			return msg
		}
		return null
	}
}
