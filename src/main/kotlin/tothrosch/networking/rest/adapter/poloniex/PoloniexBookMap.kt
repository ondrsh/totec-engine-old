package tothrosch.networking.rest.adapter.poloniex

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpResponse
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Book

class PoloniexBookMap : HashMap<Instrument, JsonNode>() {
	var lastReceived: Long = 0


	fun responseToMap(exchange: Exchange, mapper: ObjectMapper, httpResponse: HttpResponse) {
		this.clear()
		val responseNode: JsonNode = mapper.readTree(httpResponse.entity.content)
		for ((key, value) in responseNode.fields()) {
			this.put(exchange.symToInst[key]!!, value)
		}
		lastReceived = System.currentTimeMillis()
	}

	fun getBookSnapshot(mapper: ObjectMapper, instrument: Instrument): Book? {
		val node: JsonNode? = super.get(instrument)
		node?.let {
			this.remove(instrument)
			return mapper.treeToValue(it, Book::class.java)
		}
		return null
	}
}