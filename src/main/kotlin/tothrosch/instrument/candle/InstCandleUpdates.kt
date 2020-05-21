package tothrosch.instrument.candle

// could be multiple if different samplers are triggered at the same time
class InstCandleUpdates(private val updates: List<InstCandle<Double>> = listOf()) : List<InstCandle<Double>> by updates