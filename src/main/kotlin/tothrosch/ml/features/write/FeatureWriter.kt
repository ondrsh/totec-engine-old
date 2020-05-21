package tothrosch.ml.features.write

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tothrosch.instrument.candle.samplers.SampleType
import tothrosch.ml.features.Feature
import tothrosch.ml.features.Label
import tothrosch.ml.features.Step
import tothrosch.ml.features.StepReceiver
import tothrosch.ml.features.strategy.StrategyName
import tothrosch.settings.Settings
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.util.*
import kotlinx.coroutines.isActive as isActive1

class FeatureWriter(strategyName: StrategyName, sampleType: SampleType) : StepReceiver<Double, Double>(strategyName, sampleType) {
	private var isCurrentlyValid = false
	private var lastStrideBroken: Instant = Instant.now()
	private var lastStrideStarted: Instant = Instant.now()
	private var initialized = false
	private var nFeatures = 0
	private var nLabels = 0
	private var stepWidth = 0
	// we will add more LinkedLists later to buffer during initializing (when we know how many features/labels)
	// bufferLengths has always to be EXACTLY Settings.expLength + 1
	// bufferLength are callable over buffer.length
	// TODO make a class called Buffer that provides more intuitive functionality
	val featureBuffer: MutableList<LinkedList<Feature<out Any>>> = createEmptyBuffer()
	val labelBuffer: MutableList<LinkedList<Feature<out Any>>> = createEmptyBuffer()
	var lastTimeStep = Instant.now()
	val path = Settings.deepLearningPath + File.separator + "main"
	val file = File(path + File.separator + strategyName +  ".csv")
	lateinit var bw: BufferedWriter
	var count = 0
	
	// TODO close bw here, indem man die "letzte" message vom instrumentReader durch alle durchreicht
	override fun receiveStep(step: Step<Double, Double>) {
		lastTimeStep = Instant.now()
		if (initialized == false) {
			initialize(step.features, step.labels)
			initialized = true
		}
		
		if (step.features.size != nFeatures || step.labels.size != nLabels) {
			throw RuntimeException("step has different input size than features + labels")
		}
		
		if (isCurrentlyValid == false) {
			isCurrentlyValid = true
			lastStrideStarted = Instant.now()
			println("have valid steps again, downtime was ${(lastStrideStarted.toEpochMilli() - lastStrideBroken.toEpochMilli() * 1.0) / 1000} seconds")
		}
		
		// we only write old steps
		if (count % Settings.labelStride == 0 && labelBuffer.length == Settings.labelExpLength + 1) {
			writeOldStep()
		}
		addStepToBuffer(step.features, step.labels)
		checkAndShortenBuffers()
		count++
	/*	if (!isOnStreak) {
			log(now, "streak starting, downtime was ${(now - lastStrideBroken) / 1000.0} seconds")
			isOnStreak = true
			lastStrideStarted = now
		}*/
	}
	
	override fun invalidStep() {
		if (isCurrentlyValid) {
			isCurrentlyValid = false
			lastStrideBroken = Instant.now()
			println("received invalid step, had run for ${(lastStrideBroken.toEpochMilli() - lastStrideStarted.toEpochMilli() * 1.0) / 1000} seconds")
			clearBuffer()
		}
	}
	
	fun initialize(features: List<Feature<out Any>>, labels: List<Label<out Any>>) {
		createFileAndDir(file)
		bw = BufferedWriter(FileWriter(file))
		nFeatures = features.size
		nLabels = labels.size
		stepWidth = nFeatures + nLabels
		addBufferLists()
		writeHeader(features, labels)
	}
	
	fun addBufferLists() {
		repeat(nFeatures) { featureBuffer.add(LinkedList())}
		repeat(nLabels) { labelBuffer.add(LinkedList())}
	}
	
	
	fun addStepToBuffer(features: List<Feature<out Any>>, labels: List<Label<out Any>>) {
		for (i in features.indices) {
			featureBuffer[i].add(features[i])
		}
		for (i in labels.indices) {
			labelBuffer[i].add(labels[i])
		}
	}
	
	fun checkAndShortenBuffers() {
		checkAndShortenBuffer(featureBuffer)
		checkAndShortenBuffer(labelBuffer)
	}
	
	
	fun checkAndShortenBuffer(buffer: List<LinkedList<out Any>>) {
		var numDeleted = 0
		while (buffer.length > Settings.labelExpLength + 1) {
			for (i in 0..buffer.size-1) {
				buffer[i].removeFirst()
			}
			numDeleted++
		}
		if (numDeleted > 1) {
			println("you have a bug, bufferLength too High")
		}
	}
	
	// TODO remove casting by generalizing somehow
	fun writeOldStep() {
		val featuresToWrite = featureBuffer.map { it[0].value }
		@Suppress("UNCHECKED_CAST")
		val labelsToWrite = labelBuffer.map { (it.drop(1).map { it.value } as List<Double>).inverseExponential() }
		bw.write(featuresToWrite.joinToString(",") + "," + labelsToWrite.joinToString(",") + System.lineSeparator())
	}
	
	fun createEmptyBuffer() = arrayListOf<LinkedList<Feature<out Any>>>()
	
	fun clearBuffer() {
		featureBuffer.forEach { it.clear() }
		labelBuffer.forEach { it.clear() }
	}
	
	fun writeHeader(features: List<Feature<out Any>>, labels: List<Label<out Any>>) = bw.write(features.joinToString(",") + "," + labels.joinToString(",") + System.lineSeparator())
	
	suspend fun waitForIdle() {
		while (lastTimeStep.plusSeconds(5) > Instant.now()) {
			delay(500)
		}
	}
	
	
	fun createFileAndDir(file: File) = createFilesAndDirs(listOf(file))
	
	fun createFilesAndDirs(files: List<File>) {
		for (file in files) {
			if (file.exists() == false) file.parentFile.mkdirs()
			if (file.exists()) file.delete()
			file.createNewFile()
		}
	}
	
	private fun List<Double>.inverseExponential(): Double {
		val reverse = this.asReversed()
		val factor = 2.0 / (1 + reverse.size)
		var last = reverse[0]
		var count = 1
		while (count < reverse.size) {
			last += factor * (reverse[count] - last)
			count++
		}
		return last
	}
	
	private val List<LinkedList<out Any>>.length: Int
		get() = this[0].size
}