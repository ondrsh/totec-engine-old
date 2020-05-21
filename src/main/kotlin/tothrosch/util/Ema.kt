package tothrosch.util

class Ema(emaLength: Int, initializingValue: Double) {

	private val factor = 2.0 / (emaLength + 1)
	var value = initializingValue

	fun add(newValue: Double) {
		value += factor * (newValue - value)
	}

}