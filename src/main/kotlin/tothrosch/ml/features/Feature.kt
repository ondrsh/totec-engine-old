package tothrosch.ml.features

import tothrosch.engine.Keep
import java.lang.IllegalArgumentException

data class Feature<T>(val name: String, val value: T) {
	override fun toString() = name
}

operator fun Feature<Double>.plus(other: Feature<Double>): Feature<Double> {
	if (this.name == other.name) {
		return Feature(this.name, this.value + other.value)
	} else throw IllegalArgumentException("Tried to add feature $this and feature $other together")
}

