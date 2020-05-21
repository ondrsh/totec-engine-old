package tothrosch.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import tothrosch.engine.Keep
import java.io.IOException

@Keep
class NullStringDeserializer : JsonDeserializer<String>() {
	
	@Throws(IOException::class, JsonProcessingException::class)
	override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): String? {
		val node = jsonParser.readValueAs(String::class.java)
		return if (node.isEmpty()) {
			null
		} else node.toString()
	}
	
}