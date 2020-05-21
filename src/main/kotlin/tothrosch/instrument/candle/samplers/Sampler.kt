package tothrosch.instrument.candle.samplers

import tothrosch.engine.message.Message
import tothrosch.exchange.currencies.api.FiatRates
import tothrosch.instrument.Instrument
import tothrosch.instrument.Trades
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.instrument.candle.*
import tothrosch.util.AppScope
import tothrosch.util.log
import tothrosch.util.time.TimeScope
import java.util.*

abstract class Sampler(val instrument: Instrument) : AppScope, TimeScope by instrument  {
	
	// make this dependent on something
	private val meltAmount = 10_000.0
	private var currentUsdFactor: Double = getUsdFactor()
	private val currentOpsQueue = ArrayDeque<Message<BookOperations>>(256)
	private val currentTradeQueue = ArrayDeque<Message<Trades>>(256)
	// the first one is for us to update
	open val container = QueueContainer(instrument)
	// but we send the second one to CandleHub
	open val containerClone = QueueContainer(instrument)
	abstract val sampleType: SampleType
	
	suspend fun register() {
		CandleHub.channel.send(Message(CandleRegistration(sampleType, containerClone)))
	}
	
	// TODO base this on running stuff so less computational effort
	protected abstract fun sampleDecision(): Boolean
	
	fun addOpsMsg(opsMsg: Message<BookOperations>): InstCandle<Double>? {
		currentOpsQueue.addLast(opsMsg)
		return tryToGetCandleUpdate()
	}
	
	fun addTradesMsg(tradesMsg: Message<Trades>): InstCandle<Double>? {
		currentTradeQueue.addLast(tradesMsg)
		return tryToGetCandleUpdate()
	}
	
	// important: if no sampling is done, we return null
	// otherwise, we return either the CandleUpdate with the candle (if
	// there weren't any errors) or the CandleUpdate with an empty candle (if
	// there were error while adding)
	private fun tryToGetCandleUpdate(): InstCandle<Double>? {
		if (instrument.consistentForFeatures() == false) {
			container.resetAll()
			clearQueues()
			container.hasValidIndicators = false
			return InstCandle(sampleType, Candle(), container)
		}
		
		return if (sampleDecision()) {
			val update = sample()
			clearQueues()
			return update
		} else null
	}
	
	
	private fun sample(): InstCandle<Double> {
		currentUsdFactor = getUsdFactor()
		val success = container.addIndicators(opsMsgs = currentOpsQueue,
		                                      tradesMsgs = currentTradeQueue,
		                                      usdFactor = getUsdFactor())
		return if (success) {
			val candle = Candle(container.map { it.last })
			if (candle.hasNulls) {
				throw RuntimeException("candle $candle has nulls.. that should never happen")
			}
			InstCandle(sampleType = sampleType,
			           candle = candle,
			           containerClone = containerClone)
		} else {
			log("sampling was unsuccessful")
			InstCandle(sampleType, Candle(), containerClone)
		}
	}
	
	private fun clearQueues() {
		currentTradeQueue.clear()
		currentOpsQueue.clear()
	}

	
	//TODO IMPORTANT stuff might break, think about this
	private fun getUsdFactor(): Double =
		if (instrument.pair.quote.isFiat) FiatRates.getUsdRate(instrument.pair.quote) else 1.0
	
}