package tothrosch.instrument.candle.samplers

import tothrosch.instrument.Instrument

class TimeSampler(instrument: Instrument) : Sampler(instrument) {
	override val sampleType = SampleType.TIME
	
	override fun sampleDecision(): Boolean {
		TODO("Not yet implemented")
	}
}
