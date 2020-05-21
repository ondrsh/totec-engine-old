package tothrosch.trading.handlers

import tothrosch.engine.message.Message
import tothrosch.engine.message.OrderEvent.*
import tothrosch.engine.message.OrderRequest
import tothrosch.instrument.Side
import tothrosch.instrument.handlers.FinalMessageHandler
import tothrosch.trading.Hub
import tothrosch.trading.orders.BitmexOrder
import tothrosch.trading.orders.BitmexOrders
import tothrosch.util.abs
import tothrosch.util.decay.AgeableDouble
import tothrosch.util.time.TimeScope


// TODO Nicht aufgeben mein Tothrosch!!!!!!!! du bist bald da!!!


// some rules:


class OrderHandler(private val hub: Hub) : FinalMessageHandler<BitmexOrders>(2), TimeScope by hub {
	
	// automatic orders are stored by clOrdID
	val active: HashMap<String, BitmexOrder> = HashMap()
	val pending: HashMap<String, BitmexOrder> = HashMap()
	
	// manuals are only stored by orderID
	private val manual: HashMap<String, BitmexOrder> = HashMap()
	// val manualGraveyard = BitmexOrderGraveyard(20_000, this.time)
	
	override suspend fun handleMessage(msg: Message<BitmexOrders>) {
		if (msg is OrderRequest) handleRestRequest(msg)
		else msg.content.forEach { handleFeedOrder(it) }
		
		if (msg.error != null) {
			when (msg.error!!.code) {
				// rate-limited, buy we handle this in restclient
				429 -> {
				}
				// overload
				503 -> {
					// handleOverload()
				}
			}
		}
	}
	
	// here we can get a canceled order if we have ParticipateDoNotInitiate and the amended order
	// would be triggered --> then the returning order will be canceled (and the old one too)
	// but this is handled in the logic anyway so no worries
	private fun handleRestRequest(request: OrderRequest) {
		val noError = request.error == null
		if (noError) {
			when (request.orderEvent) {
				AMEND                            -> request.forEach { it.handleAmend() }
				POST, POST_SINGLE, CLOSEPOSITION -> request.forEach { it.handlePost() }
				CANCEL, CANCELALL                -> request.forEach { it.handleCancel() }
			}
		} else {
			request.forEach { pending.remove(it.clOrdID!!) }
		}
	}
	
	// check cumQty against old order and adjust if the new one is higher
	// old order gets removed anyway, so if it was because old one got hit before websocket updated it,
	// we are adding it anyway and can forget about the websocket message that arrives later
	private fun BitmexOrder.handleAmend() {
		val pendingOrder = pending[clOrdID!!]!!
		val oldOrder = active[pendingOrder.origClOrdID!!]
		if (oldOrder != null) updatePositionIfFurtherFill(this, oldOrder)
		active.remove(pendingOrder.origClOrdID)
		pending.remove(clOrdID)
		if (isDone == false) {
			active[clOrdID] = this
		}
	}
	
	// here, nothing is in active
	private fun BitmexOrder.handlePost() {
		pending.remove(clOrdID!!)
		updatePositionIfFill(this)
		if (isDone == false) {
			active[clOrdID] = this
		}
	}
	
	private fun BitmexOrder.handleCancel() {
		pending.remove(clOrdID)
		active.remove(clOrdID)
	}
	
	private fun handleFeedOrder(order: BitmexOrder) {
		if (order.clOrdID == null) handleManualOrder(order)
		else updateOrderByFeed(order)
	}
	
	// orderID != null because clOrdID == null by definition when calling this function
	private fun handleManualOrder(order: BitmexOrder) {
		val internalOrder = manual[order.orderID!!]
		if (internalOrder != null) {
			if (order.timestamp < internalOrder.timestamp) return
			updatePositionIfFurtherFill(order, internalOrder)
			if (order.isDone) manual.remove(order.orderID)
			else manual[order.orderID] = internalOrder.updateWith(order)
		}
		// much more common, because when we order from website, there won't be an internal order
		else {
			updatePositionIfFill(order)
			if (order.isDone == false) manual[order.orderID] = order
		}
	}
	
	private fun updateOrderByFeed(order: BitmexOrder) {
		active[order.clOrdID]?.let {
			if (order.timestamp >= it.timestamp) {
				updatePositionIfFurtherFill(order, it)
				if (order.isDone) active.remove(order.clOrdID!!)
				else {
					active[order.clOrdID!!] = it.updateWith(order)
				}
			}
		}
	}
	
	private fun updatePositionIfFurtherFill(newOrder: BitmexOrder, oldOrder: BitmexOrder) {
		val toAdjustAbs = (newOrder.cumQty ?: 0) - (oldOrder.cumQty ?: 0)
		if (toAdjustAbs > 0) {
			val toAdjustMaybeNegative = if (oldOrder.side == Side.BUY) toAdjustAbs else -1 * toAdjustAbs
			hub.adjustPosition(toAdjustMaybeNegative)
			if (oldOrder.isMaker) {
				addBoughtAndSold(toAdjustMaybeNegative)
			}
		}
	}
	
	private fun updatePositionIfFill(order: BitmexOrder) {
		order.cumQty?.let {
			if (it <= 0) return
			val toAdjustMaybeNegative = if (order.side == Side.BUY) order.cumQty else -1 * order.cumQty
			hub.adjustPosition(toAdjustMaybeNegative)
			if (order.isMaker) {
				addBoughtAndSold(toAdjustMaybeNegative)
			}
		}
	}
	
	private fun addBoughtAndSold(amountMaybeNegative: Int) {
		if (amountMaybeNegative > 0) {
			hub.justBoughtWithMakers.add(AgeableDouble(amountMaybeNegative.toDouble(), now))
			println("just bought $amountMaybeNegative with makers")
		} else {
			hub.justSoldWithMakers.add(AgeableDouble(amountMaybeNegative.toDouble().abs, now))
			println("just sold ${amountMaybeNegative.abs} with makers")
		}
	}
}
