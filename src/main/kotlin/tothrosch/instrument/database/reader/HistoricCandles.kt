package tothrosch.instrument.database.reader

import tothrosch.settings.Settings
import tothrosch.util.MaxSizeArrayDeque
import tothrosch.util.time.TimeScope

/*
class HistoricCandles(timeScope: TimeScope):
	MaxSizeArrayDeque<HistoricCandle>(Settings.historicCandleAmount),
	TimeScope by timeScope {
	
	var timeLastAdded: Long = 0L
	
	override fun add(element: HistoricCandle) {
		super.add(element)
		timeLastAdded = now
	}
	
}*/
