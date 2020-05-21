package tothrosch.instrument.database.reader

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import tothrosch.engine.Global
import tothrosch.engine.mode
import tothrosch.instrument.Mode
import tothrosch.instrument.database.DatabaseException
import tothrosch.settings.Settings
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

object DataReader : TimeScope, CoroutineScope {
	
	override val time: Time = Time.Read()
	override val coroutineContext: CoroutineContext = Settings.appContext
	val broadCastChannel: BroadcastChannel<Long> = BroadcastChannel(1)
	val feedbackChannel: Channel<Unit> = Channel()
	@get:JvmName("strategies")
	val instrumentReaders = mutableListOf<InstrumentReader>()
	val synchronizer = ReadSynchronizer()
	
	val job = launch(start = CoroutineStart.LAZY) {
		instrumentReaders.forEach{ it.job.start() }
		delay(3000) // needed so instrument can determine their active status
		val timerStart = System.currentTimeMillis()
		var hours = 0
		val startTime = Settings.Backtest.startLong // instrumentReaders.map { it.currentStartTime + it.currentLineElements[0].toLong() }.min()  ?: throw
		val maxInstrumentEndTime = instrumentReaders.map { it.endTime }.max() ?: throw DatabaseException("datareader could not set endTime")
		val endTime = min(maxInstrumentEndTime, Settings.Backtest.endLong)
		
		val pause = Duration.ofNanos(10)
		synchronizer.currentMs.set(startTime+1)
		
		for (i: Long in startTime..endTime) {
			time.update(i)
			if (i % Settings.flushInterval == 0L) {
				// println("datareader sending broadCastChannel")
				broadCastChannel.send(i)
				// println("datareader waiting for Individuals")
				waitForIndividuals()
				// TODO test this specifically
				// delay(1L)
				
				
				// delete inactive instruments
				// globalInstruments.removeIf { it.isActive == false }
				// instrumentReaders.removeIf { it.instrument.isActive == false }
				// println("datareader now updating time")
				time.update(i)
				// println("datareader about to flushBlocking")
				// GlobalFeatures.flushBlocking()
				// println("datareader end")
				assert(instrumentReaders.map { it.instrument.time.now }.distinct().size == 1) { "Instruments have differing times, but they should all be same after flushing"}
			}
			if (i > startTime && (i - startTime) % (3600 * 10_000) == 0L) {
				println("${10 * ++hours} hours passed, took ${(System.currentTimeMillis() - timerStart) * 1.0 / 1000} seconds")
			}
		}
		println("Sending finished")
		val tookMs = System.currentTimeMillis() - timerStart
		println("Closing Broadcastchannel, took ${ tookMs / 1000.0} seconds")
		println("one op took ${tookMs * 1.0 / instrumentReaders
			.map { it.instrument.book.bidsMutable.debug + it.instrument.book.asksMutable.debug }
			.sum()} ms")
		println("loading took ${instrumentReaders.map { it.loadingMs }.sum()}")
		delay(5000)
		broadCastChannel.close()
		return@launch
	}
	
	private suspend fun waitForIndividuals() {
		for (k in 1..Global.instruments.size) {
			feedbackChannel.receive()
		}
		if (feedbackChannel.isEmpty == false) {
			throw RuntimeException("Error - Inconsistency in GlobalFeatures class. Flushing Command")
		}
	}
	
	
}