package tothrosch.instrument.handlers.bookhandlers

import tothrosch.engine.message.Message
import tothrosch.engine.mode
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.util.time.TimeScope

class FinalOpsProcessorNormal(private val instrument: Instrument) : FinalOpsProcessor, TimeScope by instrument {
	override fun processFinalBookOps(finalOpsMsg: Message<BookOperations>): Message<BookOperations>? {
		if (mode.needsSamplers) instrument.samplers.addOpsMsg(finalOpsMsg)
		return finalOpsMsg
	}
}