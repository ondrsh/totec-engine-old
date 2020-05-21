package tothrosch.json.bitmex

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import tothrosch.engine.Keep
import tothrosch.exchange.currencies.Currency
import java.io.IOException

@Keep
class BitmexCurrencyDeserializer() : JsonDeserializer<Currency>() {
	
	@Throws(IOException::class, JsonProcessingException::class)
	override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): Currency {
		val node = jsonParser.readValueAs(String::class.java).replace("XBT", "BTC")
		if (node == "") return Currency.BTC
		return Currency.valueOf(node)
	}
}