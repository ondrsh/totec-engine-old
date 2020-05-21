package tothrosch.util

import tothrosch.ml.features.global.TimeDecayComparator
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope

class MaxSizeTimeDecayTreeSet(maxSize: Int, private val maxAge: Long, override val time: Time) : TimeScope,
	MaxSizeTreeSet<Pair<Double, Long>>(maxSize, TimeDecayComparator(maxAge, time), time) {
	
	val highestRaw: Double
		get() = innerList.first().raw()
	
	val highestWeighted: Double
		get() = innerList.first().weighted()
	
	val lowestRaw: Double
		get() = innerList.last().raw()
	
	val lowestWeighted: Double
		get() = innerList.last().weighted()
	
	
	fun averageWeighted() = innerList.map { it.weighted() }.average()
	
	fun Pair<Double, Long>.weighted() = this.first * Math.max((maxAge - this.second.ago) / (1.0 * maxAge), 0.0)
	fun Pair<Double, Long>.raw() = this.first
}