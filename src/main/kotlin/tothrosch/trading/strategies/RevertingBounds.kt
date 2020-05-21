package tothrosch.trading.strategies

import java.util.*

open class RevertingBounds(val timeSteps: Int) {


	var updateCount = 0L
	var lower = 0.0
	var upper = 0.0
	var lastLowerDistance = 0.0
	var lastUpperDistance = 0.0

	open fun update(factor: Double) {
		updateDiffs(factor)
		updateBounds(factor)
	}

	private fun updateDiffs(factor: Double) {
		lastLowerDistance = factor - lower
		lastUpperDistance = upper - factor
	}

	private fun updateBounds(factor: Double) {
		updateLower(factor)
		updateUpper(factor)
		updateCount++
	}

	private fun updateLower(factor: Double) {
		if (factor < lower) {
			lower = factor
		} else {
			lower += lastLowerDistance / timeSteps
		}
	}

	private fun updateUpper(factor: Double) {
		if (factor > upper) {
			upper = factor
		} else {
			upper -= lastUpperDistance / timeSteps
		}

	}


	class Delayed(timeSteps: Int, val delaySteps: Int) : RevertingBounds(timeSteps) {

		private val delayList = ArrayDeque<Double>(delaySteps)

		override fun update(factor: Double) {
			if (delayList.size < delaySteps) {
				delayList.add(factor)
			} else {
				super.update(delayList.removeFirst())
				delayList.add(factor)
			}
		}

	}
}