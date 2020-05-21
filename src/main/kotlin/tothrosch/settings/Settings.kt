package tothrosch.settings

import kotlinx.coroutines.Dispatchers
import tothrosch.engine.mode
import tothrosch.exchange.currencies.Currency
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.Instrument
import tothrosch.instrument.Mode
import tothrosch.util.time.minutes
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext

/**
 * Created by ndrsh on 06.06.17.
 */

object Settings {
	val appContext: CoroutineContext = Dispatchers.Default
	
	fun isTrading(instrument: Instrument) = (instrument is Instrument.Bitmex.Swap && instrument.pair == CurrencyPair(Currency.BTC, Currency.USD))
	
	
	val bookArraySize: Int
		get() {
			when (mode) {
				Mode.WRITE -> return 10000
				else       -> return 10000
			}
		}
	
	// reader
	val dataIntervalLength: Long = 1_000_000
	val historicCandleLength = 5 * minutes
	val historicCandleAmount = 60
	
	// features
	val indicatorLength: Int = 300
	val emaIndicatorLength: Int = 300
	val averageLength: Int = 30
	val labelStride: Int = 1
	val labelExpLength: Int = 400
	val flushInterval: Long = 1000
	
	// connection
	val restBookInterval: Long = 10_000
	val liveMaxTimeOut = 15_000
	
	// paths
	val dataPath: String = System.getProperty("user.home") + File.separator + "test" + File.separator + "raw"
	val deepLearningPath: String = System.getProperty("user.home") + File.separator + "deep"
	val currencyPath: String = System.getProperty("user.home")
	val koreaDateFormatter = DateTimeFormatter.ofPattern("yyy-MM-dd H:mm:ss")!!
	
	// trading
	val safetyBufferWhenClosing = 120
	
	
	object Backtest {
		private val startTime = LocalDateTime.of(2020, 1, 13, 12, 40) // this was my machine learning:  LocalDateTime.of(2019, 10, 31, 0, 0)
		private val endTime = LocalDateTime.of(2020, 1, 14, 22, 45) //  LocalDateTime.of(2019, 11, 8, 14, 30)
		
		val startLong
			get() = startTime.toEpochSecond(ZoneOffset.UTC) * 1000 / dataIntervalLength * dataIntervalLength
		val endLong
			get() = endTime.toEpochSecond(ZoneOffset.UTC) * 1000 / dataIntervalLength * dataIntervalLength
	}
	
	object Features {
		private val startTime = null
		private val endTime = null
		
	}
}
