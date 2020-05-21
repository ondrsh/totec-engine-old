package tothrosch.instrument.candle.samplers

import tothrosch.instrument.Instrument

class HybridSampler(instrument: Instrument) : Sampler(instrument) {
	
	override val sampleType = SampleType.HYBRID
	
	override fun sampleDecision(): Boolean {
		TODO("Not yet implemented")
	}
}