package tothrosch.instrument.candle

// return no candle if we are not sampling,
// but return empty candle is there is an error (so indicators get cleared)
class Candle<T>(val elements: List<T> = emptyList()) {
	
	val hasNulls: Boolean
		get() = elements.none { it == null }
	
	val isValid: Boolean
		get() = elements.isNotEmpty() && elements.none{ it == null }
	
	override fun toString(): String {
		return elements.joinToString()
	}
}
