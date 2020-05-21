package tothrosch.util.decay

import tothrosch.util.abs
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import tothrosch.util.time.seconds
import java.util.*

class DecayList(val maxMembers: Int, private val maxMinutes: Int, private val addBreakSeconds: Int, override val time: Time) : TimeScope  {
	
	private var lastAddedTime = 0L
	private var lastAddedIndex = 0
	// always sort descending, so index = 0 will have lowest item
	private val sortedList: LinkedList<DecayValue> = LinkedList()
	val values: List<Double>
		get() = sortedList.map { it.value }
	private var lastSorted = 0L
	
	fun tryAdd(nowValue: Double) {
		val absValue = nowValue.abs
		if (sortedList.size < maxMembers) {
			val decayValue = DecayValue(absValue, maxMinutes, time)
			sortedList.addFirst(decayValue)
			sortedList.sort()
			lastSorted = now
			lastAddedTime = now
			// here lastAddedIndex doesn't really matter... we just stuff in elements
		} else {
			if (lastAddedTime.ago < addBreakSeconds * seconds) {
				if (absValue > sortedList[lastAddedIndex].value) {
					val decayValue = DecayValue(absValue, maxMinutes, time)
					sortedList[lastAddedIndex] = decayValue
					lastAddedTime = now
					sortedList.sort()
					lastAddedIndex = sortedList.indexOf(decayValue)
					lastSorted = now
				}
			} else {
				if (absValue > sortedList[0].value) {
					val decayValue = DecayValue(absValue, maxMinutes, time)
					sortedList[0] = decayValue
					lastAddedTime = now
					sortedList.sort()
					lastAddedIndex = sortedList.indexOf(decayValue)
					lastSorted = now
				}
			}
		}
	}
	
}