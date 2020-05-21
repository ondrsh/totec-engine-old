package tothrosch.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import tothrosch.engine.Keep
import java.time.Instant

@Keep
class TimestampDeserializer : JsonDeserializer<Instant>() {
	
	// @Throws(IOException::class, JsonProcessingException::class)
	override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): Instant? {
		val node = jsonParser.readValueAs(String::class.java)
		return if (node.isEmpty()) null else Instant.parse(node)
	}
}

@Keep
class TimestampSerializer : JsonSerializer<Instant>() {
	
	override fun serialize(value: Instant?, gen: JsonGenerator?, serializers: SerializerProvider?) {
		val string = value?.toString()
		gen!!.writeString(string)
	}
}



