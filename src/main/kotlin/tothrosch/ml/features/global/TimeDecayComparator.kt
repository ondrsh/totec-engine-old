package tothrosch.ml.features.global

import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope


// large values = SMALL, so that they are first in the treeset
class TimeDecayComparator(val maxAge: Long, override val time: Time) : Comparator<Pair<Double, Long>>, TimeScope {
	override fun compare(p0: Pair<Double, Long>, p1: Pair<Double, Long>): Int {
		return Math.signum(p1.first * (maxAge - p1.second.ago) / (1.0 * maxAge) - p0.first * (maxAge - p0.second.ago) / (1.0 * maxAge))
			.toInt()
	}
}