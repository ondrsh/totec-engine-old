package tothrosch.trading.handlers


/*
class IndicatorMessageHandler(val trading: Trading, private val channel: Channel<Message<Pair<Exchange.Impl, Indicators>>> = Channel(10)): Channel<Message<Pair<Exchange.Impl, Indicators>>> by channel, MessageHandler<Pair<Exchange.Impl, Indicators>> {

	val indicatorMap: HashMap<Exchange.Impl, Indicators?> = hashMapOf()

	suspend override fun handleMessage(msg: Message<Pair<Exchange.Impl, Indicators>>): Message<Pair<Exchange.Impl, Indicators>>? {
		if (msg.content == null) {
			throw RuntimeException("Error at IndicatorMessageHandler - pair <Impl, Indicators> should never be null")
		}
		indicatorMap.put(msg.content.first, msg.content.second)
		return msg
	}
}*/
