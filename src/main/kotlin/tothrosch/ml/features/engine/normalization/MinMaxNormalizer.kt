package tothrosch.ml.features.engine.normalization

class MinMaxNormalizer(var min: Double = 0.0, var max: Double = 0.0) : Normalizer() {

	var range = 0.0

	override fun fit(list: List<Double>) {
		min = list.min()!!
		max = list.max()!!
		range = max - min
	}

	override fun transform(double: Double) = (double - min) / range

	override fun toString(): String {
		return "minmax,$min,$max,$range"
	}
}