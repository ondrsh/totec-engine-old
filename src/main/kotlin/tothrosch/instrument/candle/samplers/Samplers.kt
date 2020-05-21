package tothrosch.instrument.candle.samplers

import tothrosch.engine.message.Message
import tothrosch.instrument.Instrument
import tothrosch.instrument.Trades
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.instrument.candle.CandleHub
import tothrosch.instrument.candle.InstCandleUpdates
import tothrosch.instrument.candle.InstCandle
import tothrosch.util.quickSend
import tothrosch.util.time.log.Event

class Samplers(instrument: Instrument) {
	
	val samplers: Set<Sampler> = createSamplers(instrument)
	
	suspend fun initialize() = samplers.forEach { it.register() }
	
	fun addOpsMsg(opsMsg: Message<BookOperations>) {
		val instCandles = samplers.mapNotNull { it.addOpsMsg(opsMsg) }
		if (instCandles.isNotEmpty()) sendToCandleHub(opsMsg, instCandles)
	}
	
	fun addTradesMsg(tradesMsg: Message<Trades>) {
		val instCandles = samplers.mapNotNull { it.addTradesMsg(tradesMsg) }
		if (instCandles.isNotEmpty()) sendToCandleHub(tradesMsg, instCandles)
	}
	
	private fun sendToCandleHub(msg: Message<*>, instCandles: List<InstCandle<Double>>) {
		@Suppress("UNCHECKED_CAST")
		(msg as Message<Any>).also {
			it.addEvent(Event.SEND_TO_CANDLEHUB)
			it.content = InstCandleUpdates(instCandles)
			CandleHub.channel.quickSend(it)
		}
	}
	
	private fun createSamplers(instrument: Instrument): Set<Sampler> {
		val set = mutableSetOf<Sampler>()
		set.add(VolumeSampler(instrument))
		set.add(HybridSampler(instrument))
		set.add(TimeSampler(instrument))
		return set
	}
}