package tothrosch.ml.features

import tothrosch.instrument.Instrument
import tothrosch.ml.features.strategy.FeatureCreator
import java.time.Instant

abstract class BitmexFeatureCreator(val instrument: Instrument): FeatureCreator<Double, Double> {
	
	
	/*// call function receiveStep() on this object
	override lateinit var stepReceiver: StepReceiver<Double, Double>
	
	lateinit var coinbase: Instrument
	lateinit var bitfinex: Instrument
	lateinit var binance: Instrument
	lateinit var kraken: Instrument
	var isInitialized = false
	
	
	var lastPrinted = Instant.ofEpochSecond(0L)
	
	//TODO solve the problem with stepReceiver and initializing more elegantly
	
	protected fun List<Feature<Double>>.isClean(): Boolean {
		for (i in this.indices) {
			if (this[i].value.isNaN() || this[i].value.isInfinite()) {
				if (Instant.now().isAfter(lastPrinted.plusSeconds(2))) {
					println("feature ${this[i].name} is not clean. value is ${this[i].value}")
					lastPrinted = Instant.now()
				}
				return false
			}
		}
		return true
	}*/
}