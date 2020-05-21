package tothrosch.json.bitmex

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import tothrosch.engine.message.OrderEvent
import tothrosch.engine.message.OrderEvent.*
import tothrosch.engine.message.OrderRequest
import tothrosch.instrument.Side
import tothrosch.trading.orders.BitmexOrder
import tothrosch.trading.orders.BitmexOrders


// TODO test this whole crap, what a bunch of shit
class BitmexOrderRequestSerializer : JsonSerializer<OrderRequest>() {
	
	override fun serialize(value: OrderRequest, gen: JsonGenerator?, serializers: SerializerProvider?) {
		gen!!.writeStartObject()
		when (value.orderEvent) {
			POST_SINGLE -> gen.writePostSingleRequest(value)
			POST        -> gen.writePostRequest(value)
			AMEND       -> gen.writeAmendRequest(value)
			CANCEL      -> gen.writeStringField("clOrdID", value.content.map { it.clOrdID }.joinToString(","))
			CANCELALL   -> gen.writeCancelAllRequest(value)
		}
		gen.writeEndObject()
	}
	
	private fun JsonGenerator.writePostSingleRequest(request: OrderRequest) {
		val order = request.content[0]
		writeStringField("clOrdID", order.clOrdID!!)
		writeNumberField("price", order.price!!)
		writeNumberField("orderQty", order.orderQty!!)
		writeObjectField("side", order.side!!)
		writeStringField("symbol", order.symbol!!)
		writeObjectField("ordType", order.ordType!!)
		writeObjectField("timeInForce", order.timeInForce!!)
		writeObjectField("execInst", order.execInst!!)
	}
	
	private fun JsonGenerator.writePostRequest(request: OrderRequest) {
		writeArrayFieldStart("orders")
		request.content.forEach { writePostOrder(it) }
		writeEndArray()
	}
	
	private fun JsonGenerator.writeAmendRequest(request: OrderRequest) {
		writeArrayFieldStart("orders")
		request.content.forEach { writeAmendOrder(it) }
		writeEndArray()
	}
	
	private fun JsonGenerator.writeCancelAllRequest(request: OrderRequest) {
		val firstSymbol = request.content[0].symbol!!
		if (request.sameSymbols(firstSymbol)) {
			writeStringField("symbol", firstSymbol)
			val firstSide = request.content[0].side!!
			if (request.sameSide(firstSide)) {
				// TODO Test especially this!!!!!!
				writeStringField("filter", "{\"side\": ${if (firstSide == Side.BUY) "\"Buy\"" else "\"Sell\""}}")
			}
		}
	}
	
	private fun JsonGenerator.writeAmendOrder(order: BitmexOrder) {
		writeStartObject()
		writeStringField("clOrdID", order.clOrdID!!)
		writeStringField("origClOrdID", order.origClOrdID!!)
		writeNumberField("price", order.price!!)
		writeNumberField("orderQty", order.orderQty!!)
		writeEndObject()
	}
	
	private fun JsonGenerator.writePostOrder(order: BitmexOrder) {
		writeStartObject()
		writeStringField("clOrdID", order.clOrdID!!)
		writeNumberField("price", order.price!!)
		writeNumberField("orderQty", order.orderQty!!)
		writeObjectField("side", order.side!!)
		writeStringField("symbol", order.symbol!!)
		writeObjectField("ordType", order.ordType!!)
		writeObjectField("timeInForce", order.timeInForce!!)
		writeObjectField("execInst", order.execInst!!)
		writeEndObject()
	}
	
	private fun OrderRequest.sameSymbols(firstSymbol: String) = content.all { it.symbol == firstSymbol }
	
	private fun OrderRequest.sameSide(firstSide: Side) = content.all { it.side == firstSide }
}