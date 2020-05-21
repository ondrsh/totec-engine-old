package tothrosch.util

import kotlin.math.pow


class Bollinger(devs: Double, val list: List<Double>) {

	val average = list.average()
	val deviation = list.map { (it - average).pow(2.0) }.average().sqrt
	val upper = average + devs * deviation
	val lower = average - devs * deviation

	fun normalized(value: Double) = (value - lower) / (upper - lower) - 0.5
}
