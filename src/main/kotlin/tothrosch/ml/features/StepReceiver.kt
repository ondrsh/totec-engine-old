package tothrosch.ml.features

import kotlinx.coroutines.channels.Channel
import tothrosch.instrument.candle.samplers.SampleType
import tothrosch.ml.features.strategy.StrategyName

abstract class StepReceiver<T, R>(val strategyName: StrategyName, val sampleType: SampleType) {
	abstract fun receiveStep(step: Step<T,R>)
	abstract fun invalidStep()
}
