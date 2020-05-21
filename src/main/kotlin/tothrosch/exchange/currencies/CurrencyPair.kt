package tothrosch.exchange.currencies

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import tothrosch.engine.Keep

@Keep
data class CurrencyPair(val base: Currency, val quote: Currency) {
	@JsonValue
	override fun toString(): String {
		return "${base}_$quote"
	}
	
	override fun hashCode(): Int {
		var result = base.hashCode()
		result = 31 * result + quote.hashCode()
		return result
	}
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as CurrencyPair
		
		if (base != other.base) return false
		if (quote != other.quote) return false
		
		return true
	}
	
	companion object {
		@JsonCreator
		@JvmStatic
		fun fromJson(string: String): CurrencyPair {
			val splitString = string.split("_")
			val base = Currency.valueOf(splitString[0])
			val quote = Currency.valueOf(splitString[1])
			return CurrencyPair(base, quote)
		}
	}
}


