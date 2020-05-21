package tothrosch.util


fun List<Double>.weightedAverage(): Double {
	var total = 0.0
	var index = 1
	val size = this.size
	for (dbl in this) {
		total += index * dbl / size
		index++
	}
	
	return if (size % 2 == 0) {
		total / (size / 2 + 1)
	} else {
		total / ((size - 1) / 2 + 1)
	}
	
}

fun List<Double>.variance(): Double {
	val mean = this.average()
	return this.map { Math.pow(it - mean, 2.0) }.sum() / this.size
}
