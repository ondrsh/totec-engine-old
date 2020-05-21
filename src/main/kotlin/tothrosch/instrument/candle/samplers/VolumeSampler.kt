package tothrosch.instrument.candle.samplers

import tothrosch.instrument.Instrument

class VolumeSampler(instrument: Instrument) : Sampler(instrument) {
	override val sampleType = SampleType.VOL
	
	override fun sampleDecision(): Boolean {
		TODO("Not yet implemented")
	}
}