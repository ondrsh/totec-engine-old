package tothrosch.ml.features.strategy

import tothrosch.engine.mode
import tothrosch.instrument.Mode
import tothrosch.instrument.candle.CandleHub
import tothrosch.instrument.candle.QueueContainer
import tothrosch.instrument.candle.samplers.SampleType
import tothrosch.ml.features.Step
import tothrosch.ml.features.StepReceiver
import tothrosch.ml.features.write.FeatureWriter

class FeatureStrategy() : FeatureCreator<Double, Double> {
	
	val name = StrategyName.BID_ASK_1MIN
	override val stepReceivers = createDoubleStepReceivers(name)
	
	override fun predict(sampleType: SampleType, containers: Set<QueueContainer>): Step<Double, Double> {
		TODO("implement this function")
	}
	
}
