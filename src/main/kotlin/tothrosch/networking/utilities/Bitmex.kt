/*
package tothrosch.networking.utilities

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import tothrosch.instrument.Update

object Bitmex {
	
	fun transformInstrument(mapper: ObjectMapper, jsonNode: JsonNode): Update {
		val updateMap = Update()
		for (entry in jsonNode.fields()) {
			when (entry.key) {
				"volume24h"      -> updateMap.put("vol24h", entry.value.asText())
				"vwap"           -> updateMap.put("vwap", entry.value.asText())
				"highPrice"      -> updateMap.put("hp", entry.value.asText())
				"lowPrice"       -> updateMap.put("lp", entry.value.asText())
				"lotSize"        -> updateMap.put("ls", entry.value.asText())
				"tickSize"       -> updateMap.put("ts", entry.value.asText())
				"isQuanto"       -> updateMap.put("isq", entry.value.asText())
				"isInverse"      -> updateMap.put("isi", entry.value.asText())
				"makerFee"       -> updateMap.put("mf", entry.value.asText())
				"takerFee"       -> updateMap.put("tf", entry.value.asText())
				"settlementFee"  -> updateMap.put("sf", entry.value.asText())
				"capped"         -> updateMap.put("c", entry.value.asText())
				"limitDownPrice" -> updateMap.put("ldp", entry.value.asText())
				"limitUpPrice"   -> updateMap.put("lup", entry.value.asText())
				else             -> updateMap.put(entry.key, entry.value.asText())
			}
		}
		return updateMap
	}
}*/
