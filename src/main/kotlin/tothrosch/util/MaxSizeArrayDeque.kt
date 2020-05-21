package tothrosch.util

import tothrosch.ml.features.Resetable
import tothrosch.ml.features.Validatable
import tothrosch.settings.Settings
import java.util.*

open class MaxSizeArrayDeque<T>(val maxSize: Int = Settings.indicatorLength,
                                private val innerList: ArrayDeque<T> = ArrayDeque(maxSize)) : Resetable, Validatable {
	
	
	val size: Int
		get() = innerList.size
	val first: T
		get() = innerList.first
	val last: T
		get() = innerList.last
	
	
	open fun add(element: T): Boolean {
		innerList.addLast(element)
		removeHead()
		return true
	}
	
	fun clear() = innerList.clear()
	
	fun isEmpty(): Boolean = innerList.isEmpty()
	
	override fun isValid() = innerList.size == maxSize
	
	fun isFull() = innerList.size == maxSize
	
	private fun removeHead() {
		while (innerList.size > maxSize) {
			innerList.removeFirst()
		}
	}
	
	override fun reset() {
		innerList.clear()
	}
	
	
	fun descendingIterator() = innerList.descendingIterator()
	
	fun toList(): List<T> = innerList.toList()
	
	fun copy() = MaxSizeArrayDeque(maxSize, innerList.clone())
}

fun MaxSizeArrayDeque<out Number>.getAveragedLast(stride: Int): Double {
	val iter = this.descendingIterator()
	var sum = 0.0
	for (i in 1..stride) {
		sum += iter.next() as Double
	}
	return sum / stride
}

val MaxSizeArrayDeque<out Number>.lastStride: Double
	get() = this.getAveragedLast(Settings.averageLength)



