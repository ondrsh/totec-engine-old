package tothrosch.instrument.handlers.bookhandlers

import tothrosch.engine.message.Message
import tothrosch.engine.message.Request
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Book
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.util.time.log.Event

class BookHandler(instrument: Instrument) : BookableHandler<Book>(instrument) {
	
	override fun getBookOperationsMsg(msg: Message<Book>): Message<BookOperations>? {
		// if bookInit exists:
		if (instrument.bookInit != null) {
			if (instrument.bookInit.initialized == false) {
				// if book too old, get another one
				if (instrument.bookInit.msgList.isNotEmpty() && instrument.bookInit.msgList.first().content.sequence > msg.content.sequence + 1) {
					println("${instrument.exchange.name}: rest book for ${instrument.pair} too old, getting another one")
					if (instrument.bookInit.oldBookCount > 5) throw RuntimeException("Instrument Exception - too many old books from rest, oldBookCounts = ${instrument.bookInit.oldBookCount}")
					instrument.bookInit.oldBookCount++
					val restClient = instrument.exchange.restClient
					restClient.executeRequest(http = restClient.getBookSnapshotHttpRequest(instrument),
					                          request = Request(returnChannel = this.channel,
					                                         content = Book(),
					                                         timeStamp = now))
					instrument.bookInit.sending = true
					return null
				}
			/*	if (instrument.initialized) {
					val resultingOps = instrument.book.processBook(msg.content)
					resultingOps?.let {
						// send ops that were caused by book to instrumentwriter
						@Suppress("UNCHECKED_CAST")
						instrument.passToWriter((msg as Message<BookOperations>).apply {
							content = it
							timestamp = now
							addEvent(Event.BOOK_PROCESSED)
						} )
					}
				} else {*/
				// TODO Here cancel our orders
				msg.content.bidsMutable.debug = instrument.book.bidsMutable.debug
				msg.content.asksMutable.debug = instrument.book.asksMutable.debug
				instrument.book.bidsMutable = msg.content.bidsMutable
				instrument.book.asksMutable = msg.content.asksMutable
				
				/*}*/
				// TODO Here cancel our orders
				
				instrument.bookInit.oldBookCount = 0
				instrument.bookInit.snapshotSequence = msg.content.sequence
				instrument.bookInit.removeEarlyMessages()
				// replay book messages
				for (replayMsg: Message<BookOperations> in instrument.bookInit.msgList) {
					instrument.book.processBookOperations(replayMsg.content)?.let {
						replayMsg.content = it
						replayMsg.timestamp = now
						// replayMsg.addEvent(Event.REPLAY_BOOKOPS_PROCESSED)
						instrument.passToWriter(replayMsg)
					}
				}
				instrument.bookInit.msgList.clear()
				instrument.bookInit.initialized = true
				return null
			}
			return null
		} else {
			// TODO only process for important books
			return if (instrument.initialized && msg.content.time - instrument.book.time > 10_000) {
				println("processing book of instrument ${instrument.symbol}")
				instrument.book.processBook(msg.content, instrument.isTrading)?.let { ops ->
					@Suppress("UNCHECKED_CAST")
					(msg as Message<BookOperations>).apply {
						content = ops
						// addEvent(Event.BOOK_PROCESSED)
					}
				}
			} else {
				msg.content.bidsMutable.debug = instrument.book.bidsMutable.debug
				msg.content.asksMutable.debug = instrument.book.asksMutable.debug
				instrument.book.bidsMutable = msg.content.bidsMutable
				instrument.book.asksMutable = msg.content.asksMutable
				instrument.book.time = now
				null
			}
		}
}


	/*suspend fun handleBookMessage(msg: Message<out Bookable>): BookOperations? {
		when (msg.content) {
			is BookOperations -> {
				// handle possible Initialization
				if (instrument.bookInit != null) {
					if (!instrument.bookInit.initialized) {
						@Suppress("UNCHECKED_CAST")
						instrument.bookInit.msgList.add(msg as Message<BookOperations>)
						if (!instrument.bookInit.sending) {
							val restClient = instrument.exchange.restClient
							restClient.sendRequest(http = restClient.getBookSnapshotHttpRequest(instrument),
							                       request = Request(returnChannel = this.channel,
							                                         base = Book(),
							                                         created = now))
							instrument.bookInit.sending = true
						}
						return null
					} else {
						// if initialzed, but sequence number lower than orderbook, ignore message
						if (msg.content.sequence < instrument.bookInit.snapshotSequence) {
							return null
						}
					}
				}
				return instrument.book.processBookOperations(msg) //
			}
			
			is Book           -> {
			
			
			}
			
			else              -> {
				log("${instrument.exchange.name} bookChannel received message of unknown type. Message content: ${msg.content}")
				return null
			}
		}
		}*/
	
	
	/*companion object {
		fun create(instrument: Instrument) = if (instrument.exchange.name == Exchange.Name.BITMEX &&
			instrument.symbol == "XBTUSD" &&
			(mode == Mode.BACKTEST || mode == Mode.BACKTEST_LIVE)) BacktestBookMessageHandler(instrument) else BookHandler(instrument)
	}*/
}
