/*
package tothrosch.trading.json.rest

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import tothrosch.trading.exchange.currencies.ExchangeRates
import tothrosch.trading.json.JsonException

class ExchangeRatesDeserializer: StdDeserializer<ExchangeRates> {
    constructor(vc: Class<Any>?) : super(vc)
    constructor() : this(null)


    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): ExchangeRates {
        val jacksonJsonNode: JsonNode = p?.codec?.readTree(p) ?: throw JsonException ("cannot deserialize json because $p is null")

    }
}*/
