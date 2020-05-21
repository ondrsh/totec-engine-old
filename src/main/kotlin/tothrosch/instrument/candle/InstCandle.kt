package tothrosch.instrument.candle

import tothrosch.instrument.candle.samplers.SampleType

data class InstCandle<T>(val sampleType: SampleType,
                         val candle: Candle<T>,
                         val containerClone: QueueContainer) {
	
	fun isValid() = candle.isValid
}

