package tothrosch.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import tothrosch.engine.Keep
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.CurrencyPair
import java.io.IOException

@Keep
class CurrencyPairDeserializer(val exchange: Exchange) : JsonDeserializer<CurrencyPair>() {
	
	
	@Throws(IOException::class, JsonProcessingException::class)
	override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): CurrencyPair? {
		val node = jsonParser.readValueAs(String::class.java)
		return if (node.isEmpty()) {
			null
		} else exchange.symToInst[node]?.pair
	}
	
}
