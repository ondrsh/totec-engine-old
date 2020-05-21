package tothrosch.ml.features.engine.normalization

class StandardNormalizer(var mean: Double = 0.0, var std: Double = 0.0) : Normalizer() {


	override fun fit(list: List<Double>) {
		mean = list.average()
		std = Math.sqrt(list.map { Math.pow(it - mean, 2.0) }.sum() / list.size)
	}


	override fun transform(dbl: Double) = (dbl - mean) / std


	override fun toString(): String {
		return "standard,$mean,$std"
	}
}