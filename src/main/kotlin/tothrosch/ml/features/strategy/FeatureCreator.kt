package tothrosch.ml.features.strategy

import tothrosch.engine.mode
import tothrosch.instrument.Mode
import tothrosch.instrument.candle.CandleHub
import tothrosch.instrument.candle.QueueContainer
import tothrosch.instrument.candle.samplers.SampleType
import tothrosch.ml.features.Step
import tothrosch.ml.features.StepReceiver
import tothrosch.ml.features.write.FeatureWriter

// vielleicht später dann immer die gleichen paare immer an die gleichen positionen stellen,
// so wie früher... derweil mal was anderes probieren, geht leichter.

interface FeatureCreator<T, R> {
	
	val stepReceivers: Map<SampleType, StepReceiver<T, R>>
	fun predict(sampleType: SampleType, containers: Set<QueueContainer>): Step<T, R>
	
	fun createDoubleStepReceivers(name: StrategyName): Map<SampleType, StepReceiver<Double, Double>> {
		val stepReceivers = SampleType.values().associate {
			if (mode == Mode.FEATUREWRITE) it to FeatureWriter(name, it)
			else it to object : StepReceiver<Double, Double>(sampleType = it,
			                                                 strategyName = name) {
				
				override fun receiveStep(step: Step<Double, Double>) {
					CandleHub.preds[strategyName]!![sampleType] = step.labels
				}
				
				override fun invalidStep() {}
			}
		}
		return stepReceivers
	}
}