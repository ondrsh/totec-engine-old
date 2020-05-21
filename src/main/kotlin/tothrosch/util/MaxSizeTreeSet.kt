package tothrosch.util

import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import java.util.*

//
open class MaxSizeTreeSet<T>(private val maxSize: Int, private val comparator: Comparator<T>, override val time: Time) : TimeScope {
	
	protected val innerList = arrayListOf<T>()
	private var lastSorted: Long = 0L
	val first: T
		get() = innerList.first()
	
	val last: T
		get() = innerList.last()
	
	fun add(element: T): Boolean {
		if (innerList.size < maxSize) {
			innerList.add(element)
			return true
		}
		if (comparator.compare(element, innerList.last()) == -1) {
			while (innerList.size >= this.maxSize) {
				innerList.remove(innerList.last())
			}
			innerList.add(element)
			update()
			return true
		}
		return false
	}
	
	fun addAll(elements: Collection<T>): Boolean {
		var addedAtLeastOne = false
		for (element in elements) {
			addedAtLeastOne = addedAtLeastOne || add(element)
		}
		return addedAtLeastOne
	}
	
	fun update() {
		if (lastSorted.ago > 5000) {
			innerList.sortWith(comparator)
			lastSorted = now
		}
	}
}
