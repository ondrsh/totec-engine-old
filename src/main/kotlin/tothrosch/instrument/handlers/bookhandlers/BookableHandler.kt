package tothrosch.instrument.handlers.bookhandlers

import tothrosch.engine.message.Message
import tothrosch.engine.mode
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Bookable
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.instrument.handlers.MessageHandler
import tothrosch.util.time.TimeScope

abstract class BookableHandler<T : Bookable>(val instrument: Instrument) : MessageHandler<T, BookOperations>(1000), TimeScope by instrument {
	
	// if there are operations from me, forward them to backtesting
	private val finalOpsProcessor: FinalOpsProcessor = if (mode.isBacktesting && instrument.isTrading) FinalOpsProcessorBacktest(
		instrument)
	else FinalOpsProcessorNormal(instrument)
	
	
	protected abstract fun getBookOperationsMsg(msg: Message<T>): Message<BookOperations>?
	
	override fun handleMessage(msg: Message<T>): Message<BookOperations>? {
		time.update(msg.timestamp)
		// this is where the actual changing is done
		val bookOps = getBookOperationsMsg(msg)
		
		// we update the book time after we change stuff, because we might need the time delta
		instrument.isActive = true
		instrument.book.time = msg.timestamp
		instrument.exchange.liveFeed.lastBookMessage = msg.timestamp
		instrument.book.updateImmutable20()
		
		return bookOps?.let { finalOpsProcessor.processFinalBookOps(it) }
	}
}