package tothrosch.util.decay

import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope


class DecayValue(val rawValue: Double, maxMinutes: Int, override val time: Time) : Comparable<DecayValue>, TimeScope  {
	val born = now
	val maxAge = 1000L * 60L * maxMinutes
	// values are ALWAYS absolute values
	val value: Double
		get() = rawValue * (maxAge - (now - born)) / maxAge
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as DecayValue
		
		if (rawValue != other.rawValue) return false
		if (born != other.born) return false
		
		return true
	}
	
	override fun hashCode(): Int {
		var result = rawValue.hashCode()
		result = 31 * result + born.hashCode()
		return result
	}
	
	override fun compareTo(other: DecayValue): Int {
		if (this.value > other.value) return 1
		if (this.value < other.value) return -1
		return 0
	}
}