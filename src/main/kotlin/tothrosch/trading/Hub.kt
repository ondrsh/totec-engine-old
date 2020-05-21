package tothrosch.trading

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import tothrosch.engine.Global
import tothrosch.engine.message.*
import tothrosch.instrument.Side
import tothrosch.instrument.candle.CandleHub
import tothrosch.instrument.candle.CandleRegistration
import tothrosch.instrument.candle.InstCandleUpdates
import tothrosch.ml.features.Label
import tothrosch.settings.Settings
import tothrosch.trading.adapters.TradingAdapter
import tothrosch.trading.handlers.OrderHandler
import tothrosch.trading.handlers.PositionMessageHandler
import tothrosch.trading.orders.BitmexOrder
import tothrosch.trading.orders.BitmexOrders
import tothrosch.trading.position.bitmex.BitmexPosition
import tothrosch.util.MaxAgeList
import tothrosch.util.decay.AgeableDouble
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope

object Hub : CoroutineScope, TimeScope {
	
	override val coroutineContext = Settings.appContext
	override val time: Time = Global.tradingInstrument.time
	
	
	@Volatile
	var position = BitmexPosition(currentQty = 0,
	                              timestamp = 0L)
	
	val adapter = TradingAdapter.create()
	
	val requestCounter: RequestCounter = RequestCounter()
	val justBoughtWithMakers = MaxAgeList<AgeableDouble>(20_000, time)
	val justSoldWithMakers = MaxAgeList<AgeableDouble>(20_000, time)
	
	val orderHandler = OrderHandler(this)
	val positionHandler = PositionMessageHandler(this)
	
	// val myTradesBacktestHandler = MyTradesBacktestHandler(this)
	val trydeTrigger = Channel<Message<Unit>>(CONFLATED)
	
	var labels: List<Label<Double>> = listOf()
	var overload = false
	
	val inst = Global.tradingInstrument
	
	val channel = Channel<Message<out Any>>(100)
	
	val job = launch(start = CoroutineStart.LAZY) {
		try {
			while (isActive) {
				for (msg in channel) {
					@Suppress("UNCHECKED_CAST")
					when (msg.content) {
						is InstCandleUpdates  -> {
						
						}
						is BitmexOrders       -> orderHandler.handleMessage(msg as Message<BitmexOrders>)
						is BitmexPosition     -> positionHandler.handleMessage(msg as Message<BitmexPosition>)
						is Unit               -> tryde()
					}
				}
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
			delay(10_000)
		}
	}
	
	private fun handleOverload() {
		if (overload == false) {
			overload = true
			launch {
				delay(500)
				trydeTrigger.send(Message(Unit))
				overload = false
			}
		}
	}
	
	// try to trade lol
	fun tryde() {
		// trading logic
		val makers = orderHandler.pending.filterValues { it.isMaker }
		
		val futRet = labels[0].value
		val book = Global.tradingInstrument.book
	}
	
	
	fun adjustPosition(toAdjust: Int) {
		position.copy(currentQty = position.currentQty + toAdjust,
		              timestamp = now)
		
		println("adjusting position by $toAdjust, new position: ${position.currentQty}")
	}
	
	fun amend(orders: BitmexOrders) {
		TODO("Not yet implemented")
		
		orderHandler.pending.putAll(orders.associateBy { it.clOrdID!! })
		adapter.sendRequest(orders.getAmendRequest())
	}
	
	fun post(orders: BitmexOrders) {
		TODO("Not yet implemented")
		
		orderHandler.pending.putAll(orders.associateBy { it.clOrdID!! })
		adapter.sendRequest(orders.getPostRequest())
	}
	
	fun postSingle(order: BitmexOrder) {
		TODO("Not yet implemented")
		
		orderHandler.pending[order.clOrdID!!] = order
		adapter.sendRequest(order.getPostSingleRequest())
	}
	
	fun cancel(orders: BitmexOrders) {
		// TODO("Not yet implemented")
		
		orderHandler.pending.putAll(orders.associateBy { it.clOrdID!! })
		adapter.sendRequest(orders.getCancelRequest())
	}
	
	fun closePosition(price: Double) {
		val order = BitmexOrder.newCloseTaker(price = price,
		                                      side = position.getClosingSide(),
		                                      symbol = inst.symbol,
		                                      now = now)
		orderHandler.pending[order.clOrdID!!] = order
		adapter.sendRequest(order.getClosePositionRequest())
	}
	
	private fun BitmexPosition.getClosingSide() = if (currentQty > 0) Side.SELL else Side.BUY
	
}