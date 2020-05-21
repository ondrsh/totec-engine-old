package tothrosch.json.rest

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import tothrosch.exchange.SupportedPairs
import tothrosch.exchange.currencies.Currency
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.json.JsonException

object SupportedPairsRestDeserializer {
	
	class Poloniex : StdDeserializer<SupportedPairs> {
		constructor(vc: Class<Any>?) : super(vc)
		constructor() : this(null)
		
		override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): SupportedPairs {
			val jacksonJsonNode: JsonNode = p?.codec?.readTree(p) ?: throw JsonException("cannot deserialize json because $p is null")
			return SupportedPairs(jacksonJsonNode.fieldNames().asSequence().mapNotNull { symToPair(it) }.toSet())
		}
		
		fun symToPair(sym: String): CurrencyPair? {
			// plz don't ask why they send QUOTE_BASE instead of BASE_QUOTE..... dumb polo
			val symQuote: String = sym.substringBefore("_").replace("USDT", "USD")
			val symBase: String = sym.substringAfter("_").replace("USDT", "USD")
			val baseCurrency: Currency? = try {
				Currency.valueOf(symBase)
			} catch (ex: Exception) {
				null
			}
			val quoteCurrency: Currency? = try {
				Currency.valueOf(symQuote)
			} catch (ex: Exception) {
				null
			}
			if (baseCurrency == null) {
				println("POLONIEX ERROR: currency $symBase not in currency-database")
				return null
			}
			if (quoteCurrency == null) {
				println("POLONIEX ERROR: currency $symQuote not in currency-database")
				return null
			}
			return CurrencyPair(base = Currency.valueOf(symBase), quote = Currency.valueOf(symQuote))
		}
	}
}