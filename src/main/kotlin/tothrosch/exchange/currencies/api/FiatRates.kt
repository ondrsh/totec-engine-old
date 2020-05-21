package tothrosch.exchange.currencies.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tothrosch.exchange.currencies.Currency
import tothrosch.settings.Settings
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

object FiatRates {
	
	private val historicalFile: File = File(Settings.currencyPath + File.separator + "currency.historical")
	private val writer: BufferedWriter = BufferedWriter(FileWriter(historicalFile, true))
	val historicalList: ArrayList<DailyFiat> = arrayListOf()
	private var currentDayStart: Long = 0L
	private var currentDayEnd: Long = 0L
	@Volatile private var currentIndex: Int = -1
	lateinit private var currentDailyFiat: DailyFiat
	lateinit private var lastDailyFiat: DailyFiat
	private var currentFactor: Double = 0.0
	
	
	init {
		readFile()
		if (historicalList.size == 0) {
			runBlocking { fillHistory(LocalDate.parse("2018-08-09")) }
		} else {
			runBlocking { fillHistory(historicalList.last().localDate) }
		}
		setNext()
	}
	
	// TODO fiat rates have to be updated somewhere when reading files
	fun update(time: Long) {
		if (time < currentDayStart) throw RuntimeException("FiatRates Error - cannot go back in time. currentDayStart: $currentDayStart, input time: $time")
		while (time > currentDayEnd && historicalList.size > currentIndex + 1) {
			setNext()
		}
		currentFactor = Math.max(0.0, Math.min(1.0 * (time - currentDayStart) / (currentDayEnd - currentDayStart), 1.0))
	}
	
	fun getUsdRate(currency: Currency) =
		if (currency == Currency.USD) 1.0 else lastDailyFiat.usdRates[currency]!! * (1 - currentFactor) + currentDailyFiat.usdRates[currency]!! * currentFactor
	
	private suspend fun fillHistory(lastDay: LocalDate) {
		var day = lastDay.plusDays(1)
		val now = LocalDate.now()
		var first = true
		while (day.isAfter(now) == false) {
			if (day.isEqual(now) && LocalDateTime.now(ZoneOffset.UTC).hour < 18) {
				return
			}
			if (first == false) {
				delay(300)
			}
			val fiatMap = FixerApi.getByDate(day)
			if (fiatMap.size > 0) {
				val dailyFiat = DailyFiat(day, fiatMap)
				historicalList.add(dailyFiat)
				writer.write(dailyFiat.toString())
				writer.flush()
				println("wrote line")
			} else {
				return
			}
			day = day.plusDays(1)
			first = false
		}
	}
	
	
	private fun readFile() = historicalFile.readLines().forEach { historicalList.add(DailyFiat.fromLine(it)) }
	
	
	private fun setNext() {
		currentIndex++
		lastDailyFiat = historicalList[Math.max(0, currentIndex - 1)]
		currentDailyFiat = historicalList[currentIndex]
		setDayStartAndEnd(currentDailyFiat)
	}
	
	private fun setDayStartAndEnd(dailyFiat: DailyFiat) {
		currentDayStart = dailyFiat.localDate.atTime(18, 0).atOffset(ZoneOffset.UTC).toEpochSecond() * 1000
		currentDayEnd = currentDayStart + 24 * 3600_000
	}
	
	
}