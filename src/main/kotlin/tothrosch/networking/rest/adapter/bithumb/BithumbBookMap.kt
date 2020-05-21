package tothrosch.networking.rest.adapter.bithumb

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpResponse
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.instrument.book.Book

/*
class BithumbBookMap : HashMap<Instrument, JsonNode>() {
	var lastReceived: Long = System.currentTimeMillis()

	fun responseToMap(exchange: Exchange, mapper: ObjectMapper, httpResponse: HttpResponse) {
		this.clear()
		val data: JsonNode = mapper.readTree(httpResponse.entity.content).get("data")
		val iter: Iterator<JsonNode> = data.iterator()
		val time: JsonNode = iter.next()
		iter.next() // field "payment currency"
		// now, all the currencies come one after another
		while (iter.hasNext()) {
			val currencyNode = iter.next()
			val customNode = mapper.createObjectNode()
			customNode.replace("time", time)
			customNode.replace("bids", currencyNode.get("bids"))
			customNode.replace("asks", currencyNode.get("asks"))
			val currency = currencyNode.get("order_currency").asText()
			if (exchange.symToInst.contains(currency)) {
				this.put(exchange.symToInst[currency]!!, customNode)
			}
		}
		lastReceived = now
	}

	fun getBookSnapshot(mapper: ObjectMapper, instrument: Instrument): Book? {
		val node: JsonNode? = super.get(instrument)
		node?.let {
			this.remove(instrument)
			return mapper.treeToValue(it, Book::class.java)
		}
		return null
	}


}*/
