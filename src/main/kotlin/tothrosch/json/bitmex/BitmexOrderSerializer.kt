package tothrosch.json.bitmex

/*
class BitmexOrderSerializer(val exchangeImpl: Exchange.Impl): JsonSerializer<BitmexOrder>() {

	override fun serialize(value: BitmexOrder?, gen: JsonGenerator?, serializers: SerializerProvider?) {
		val hasOrderID = value!!.orderID != null

		gen!!.writeStartObject()
		value.price?.let {
			gen.writeNumberField("price", it)
		}
		value.orderQty?.let {
			gen.writeNumberField("orderQty", it)
		}
		value.leavesQty?.let {
			gen.writeNumberField("leavesQty", it)
		}
		value.cumQty?.let {
			gen.writeNumberField("cumQty", it)
		}
		value.side?.let {
			gen.writeObjectField("side", it)
		}
		value.pair?.let {
			gen.writeStringField("symbol", exchangeImpl.pairToSym(it))
		}
		value.ordType?.let {
			gen.writeObjectField("ordType", it)
		}
		value.timeInForce?.let {
			gen.writeObjectField("timeInForce", it)
		}
		value.execInst?.let {
			gen.writeObjectField("execInst", it)
		}
		value.ordStatus?.let {
			gen.writeObjectField("ordStatus", it)
		}
		value.workingIndicator?.let {
			gen.writeObjectField("workingIndicator", it)
		}
		value.timestamp.let {
			gen.writeNumberField("timestamp", it)
		}


		if (!hasOrderID) {
			gen.writeStringField("clOrdID", value.clOrdID)
			gen.writeStringField("origClOrdID", value.clOrdID)
		}
		else {
			gen.writeStringField("orderID", value.orderID)
		}

		gen.writeEndObject()

	}
}*/
