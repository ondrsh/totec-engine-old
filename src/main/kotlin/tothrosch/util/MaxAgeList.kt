package tothrosch.util

import tothrosch.instrument.Ageable
import tothrosch.util.decay.AgeableDouble
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import java.util.*

class MaxAgeList<T : Ageable>(val maxAge: Long,
                              override val time: Time,
                              private val innerList: ArrayDeque<T> = ArrayDeque()
) : TimeScope {
	
	fun add(element: T) {
		innerList.addFirst(element)
		removeOld()
	}
	
	fun addAll(elements: Collection<T>) {
		innerList.addAll(elements)
		removeOld()
	}
	
	private fun removeOld() {
		while (innerList.last.time.ago > maxAge && innerList.size > 0) {
			innerList.removeLast()
		}
	}
	
	fun count() = innerList.size
	
	fun toList(): List<T> = innerList.toList()
	
	
	fun MaxAgeList<AgeableDouble>.timeWeighted(): Double {
		var sum = 0.0
		for (dbl in this.innerList) {
			val timeFactor = (maxAge - dbl.time.ago) / maxAge
			if (timeFactor > 0) {
				sum += timeFactor * dbl.value
			}
		}
		return sum
	}
}

