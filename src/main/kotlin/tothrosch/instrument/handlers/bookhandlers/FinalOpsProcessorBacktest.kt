package tothrosch.instrument.handlers.bookhandlers

import tothrosch.engine.message.Message
import tothrosch.engine.mode
import tothrosch.instrument.Instrument
import tothrosch.instrument.Side
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.trading.adapters.backtest.Backtest
import tothrosch.util.quickSend
import tothrosch.util.time.TimeScope

class FinalOpsProcessorBacktest(private val instrument: Instrument) : FinalOpsProcessor, TimeScope by instrument {
	override fun processFinalBookOps(finalOpsMsg: Message<BookOperations>): Message<BookOperations>? {
		val grouped = finalOpsMsg.content.groupBy { it.bookEntry.isMine }
		
		// if there are other ops, save them to variable as message and add them to queue
		grouped[false]?.let { others ->
			finalOpsMsg.content = BookOperations(ops = others, sequence = finalOpsMsg.content.sequence)
			if (mode.needsSamplers) instrument.samplers.addOpsMsg(finalOpsMsg)
			return finalOpsMsg
		}
		
		return null
	}
}