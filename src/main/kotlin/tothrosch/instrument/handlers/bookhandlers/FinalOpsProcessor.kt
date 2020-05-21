package tothrosch.instrument.handlers.bookhandlers

import tothrosch.engine.message.Message
import tothrosch.instrument.book.operations.BookOperations

interface FinalOpsProcessor {
		fun processFinalBookOps(finalOpsMsg: Message<BookOperations>): Message<BookOperations>?
}