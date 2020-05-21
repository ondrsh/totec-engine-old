package tothrosch.ml.features.queues

import tothrosch.ml.features.Resetable
import tothrosch.ml.features.Validatable
import tothrosch.settings.Settings
import tothrosch.util.MaxSizeArrayDeque
import tothrosch.util.MaxSizeQueue
import tothrosch.util.getAveragedLast

// innerlist = raw values
// emaList = ema values
open class EmaMaxSizeQueue(protected val emaLength: Int = Settings.emaIndicatorLength,
                           private val emaDeque: MaxSizeArrayDeque<Double> = MaxSizeArrayDeque(emaLength))
	: MaxSizeArrayDeque<Double>(Settings.indicatorLength.coerceAtLeast(emaLength)) {
	
	private val factor = 2.0 / (emaLength + 1)
	val emaLast: Double
		get() = if (emaDeque.isEmpty() == false) emaDeque.last else 0.0
	
	val emaSize: Int
		get() = emaDeque.size
	
	init {
		if (emaLength > emaDeque.maxSize) {
			throw IllegalArgumentException("EMA length cannot be longer than underlying list size")
		}
	}
	
	
	override fun add(element: Double): Boolean {
		if (element.isNaN()) return false
		super.add(element)
		addEmaTimeStep()
		return true
	}
	
	fun getRawAverage(stride: Int = Settings.averageLength) = getAveragedLast(stride)
	
	fun isEmptyEma(): Boolean = emaDeque.size == 0
	
	private fun addEmaTimeStep() {
		if (emaDeque.size == 0) {
			if (size >= emaLength) {
				initialize()
			}
			return
		} else {
			if (isEmpty()) {
				emaDeque.clear()
				return
			}
		}
		emaDeque.add(emaDeque.last + factor * (last - emaDeque.last))
	}
	
	private fun initialize() {
		val initialList = toList().takeLast(emaLength)
		var firstEma: Double = initialList.first()
		for (price in initialList) {
			firstEma += factor * (price - firstEma)
		}
		emaDeque.add(firstEma)
	}
	
	override fun reset() {
		reset()
		emaDeque.reset()
	}
	
	override fun isValid() = super.isValid() && emaDeque.size == emaLength
}


