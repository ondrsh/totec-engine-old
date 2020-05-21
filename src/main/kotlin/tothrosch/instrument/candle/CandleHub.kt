package tothrosch.instrument.candle

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import tothrosch.engine.message.Message
import tothrosch.instrument.candle.samplers.SampleType
import tothrosch.ml.features.*
import tothrosch.ml.features.strategy.FeatureStrategy
import tothrosch.ml.features.strategy.StrategyName
import tothrosch.ml.features.strategy.StrategyUpdate
import tothrosch.trading.Hub
import tothrosch.util.AppScope
import tothrosch.util.quickSend
import tothrosch.util.time.log.Event

// Each instrument has a "CandleSamplers" object, which hold multiple CandleSampler objects (one for each
// sample type). The instrument puts the messages into the samplers, and once a candle gets triggered,
// the candle (along with a clone of the queueContainer) gets sent to the updateChannel in this object.

object CandleHub : AppScope {
	
	private val strategies = listOf(FeatureStrategy())
	val preds = LinkedHashMap<StrategyName, LinkedHashMap<SampleType, List<Label<Double>>>>()
	private val containerSets: MutableMap<SampleType, ContainerSet> = createContainers()
	val channel = Channel<Message<*>>(10)
	
	val job = launch(start = CoroutineStart.LAZY) {
		for (msg in channel) {
			@Suppress("UNCHECKED_CAST")
			when (msg.content) {
				is CandleRegistration -> register(msg as Message<CandleRegistration>)
				is InstCandleUpdates  -> handleUpdate(msg as Message<InstCandleUpdates>)
			}
		}
	}
	
	// add the containerClone to the corresponding SampleType
	private fun register(msg: Message<CandleRegistration>) {
		containerSets[msg.content.sampleType]!!.add(msg.content.queueContainer)
	}
	
	// here, we actually try to add the candle to the clone... if success, we predict with all strategies
	// if this works out (valid indicators), then send to hub
	private fun handleUpdate(msg: Message<InstCandleUpdates>) {
		msg.content.forEach { update ->
			// if this 'if' is false, we automatically reset
			if (update.containerClone.addCandle(update.candle)) {
				msg.addEvent(Event.CANDLE_ADDED)
				updatePredictions(msg, containerSets[update.sampleType]!!)
			}
		}
	}
	
	// only gets executed when candles are added
	private fun updatePredictions(msg: Message<InstCandleUpdates>, containerSet: ContainerSet) {
		strategies.map { strategy ->
			msg.content.forEach { instCandle ->
				val prediction = strategy.predict(sampleType = instCandle.sampleType,
				                                  containers = containerSet)
				@Suppress("UNCHECKED_CAST")
				(msg as Message<StrategyUpdate>).also {
					it.content = StrategyUpdate(labels = prediction.labels)
					Hub.channel.quickSend(it)
				}
			}
		}
	}
	
	private fun createContainers() = SampleType.values().associate {
		it to ContainerSet(it)
	}.toMutableMap()
	
	// sum the label lists vertically together
	private fun HashMap<SampleType, List<Label<Double>>>.conflate() = values
		.asSequence()
		.reduce { listA, listB ->
			listA.zip(listB) { labelA, labelB -> labelA + labelB }
		}
}
