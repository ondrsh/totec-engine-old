package tothrosch.ml.features.engine.normalization


abstract class Normalizer {

	abstract fun fit(list: List<Double>)

	abstract fun transform(double: Double): Double
	fun transformList(list: List<Double>) = list.map { transform(it) }
}