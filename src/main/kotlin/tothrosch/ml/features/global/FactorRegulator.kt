package tothrosch.ml.features.global

import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope


class FactorRegulator(val maxAge: Long, override val time: Time) : TimeScope {

	// treeSets ranks upside down, so that biggest numbers are first
	// val factors = MaxSizeTimeDecayTreeSet(maxSize, maxAge)

	private var maxFactor: Pair<Double, Long> = 1.0 to 0L


	fun add(nowFactor: Pair<Double, Long>) {
		if (nowFactor.weighted() > maxFactor.weighted()) {
			maxFactor = nowFactor
		}
	}

	fun getFactor() = maxFactor.weighted()

	fun Pair<Double, Long>.weighted() = this.first * (maxAge - this.second.ago) / (1.0 * maxAge)

}
