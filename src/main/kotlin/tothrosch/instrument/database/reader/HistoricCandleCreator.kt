package tothrosch.instrument.database.reader

import kotlinx.coroutines.async
import tothrosch.instrument.Trades
import tothrosch.settings.Settings
import tothrosch.util.AppScope
import java.io.File

class HistoricCandleCreator(private val reader: InstrumentReader, private val fileDates: List<Long>): AppScope {
	
	private var currentIndex = -1
	private val fileDifference: Long = fileDates
		.mapIndexedNotNull { index, value ->
			if (index == 0) null
			else value-fileDates[index-1]
	}.min()!!
	private val numCandles = getNumberOfCandles()
	
	
	suspend fun getCandles(): List<HistoricCandle> {
		val tradesPerFile = fileDates.map { it to getFile(it).readLines() }
		val jobs = tradesPerFile.map { async {
			val tradeList = linesToTrades(start = it.first, lines = it.second)
			tradesToCandles(start = it.first, tradesList = tradeList)
		} }
		return jobs.map { it.await() }.flatten()
	}
	
	private fun tradesToCandles(start: Long, tradesList: List<Trades>): List<HistoricCandle> {
		var currentStart = start
		var currentEnd = start + Settings.historicCandleLength
		return (0..numCandles).map {
			val candle = HistoricCandle(start = currentStart,
			                            end = currentEnd,
			                            tradesList = tradesList
				                            .filter { it.first().time in currentStart until currentEnd })
			currentStart += Settings.historicCandleLength
			currentEnd += Settings.historicCandleLength
			candle
		}
	}
	
	private fun getNumberOfCandles() = (fileDifference / Settings.historicCandleLength).toInt()
	
	private fun linesToTrades(start: Long, lines: List<String>): List<Trades> = lines.mapNotNull {
			val split = it.split(",")
			if (split[1].first() == 'T') {
				reader.parseTrades(split[2], start + split[0].toLong())
			} else null
	}
	
	private fun getFile(long: Long) = File(reader.dir.path + File.separator + long.toString() + ".data")
	
}