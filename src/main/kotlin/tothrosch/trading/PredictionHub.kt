package tothrosch.trading

import kotlinx.coroutines.GlobalScope
import tothrosch.engine.message.Message
import tothrosch.instrument.candle.ContainerSet
import tothrosch.instrument.candle.samplers.SampleType
import tothrosch.ml.features.*
import tothrosch.ml.features.strategy.FeatureStrategy
import tothrosch.ml.features.strategy.StrategyName
import tothrosch.util.AppScope
import tothrosch.util.quickSend

/*
object PredictionHub : StepReceiver<Double, Double>() {
	
	private val strategies = listOf(FeatureStrategy())
	val preds = LinkedHashMap<StrategyName, LinkedHashMap<SampleType, List<Feature<Double>>>>()
	
	
	// only gets executed when candles are added
	fun updatePredictions(containerSet: ContainerSet): Boolean {
		if (containerSet.all { it.hasValidIndicators }) {
			strategies.forEach { it.predict(sampleType = containerSet.sampleType, containers = containerSet) }
		}
	}
	
	override fun receiveStep(sampleType: SampleType, strategyName: StrategyName, step: Step<Double, Double>) {
		preds[strategyName]!![sampleType] = step.labels
		val stratUpdates = preds.mapValues { it.value.conflate() }
		Hub.channel.quickSend(Message(stratUpdates))
	}
	
	override fun invalidStep(sampleType: SampleType, strategyName: StrategyName) {
		TODO("da noch was geiles überlegen")
	}
}

// sum the label lists vertically together
fun HashMap<SampleType, List<Label<Double>>>.conflate() = values
	.asSequence()
	.reduce { listA, listB ->
		listA.zip(listB) { labelA, labelB -> labelA + labelB }
	}
*/
