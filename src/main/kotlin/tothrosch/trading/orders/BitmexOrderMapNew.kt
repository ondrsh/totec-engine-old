/*package tothrosch.trading.orders

import tothrosch.instrument.Side
import tothrosch.trading.BitmexTradingStrategy
import tothrosch.trading.orders.BitmexOrder
import tothrosch.util.abs

class BitmexOrderMapNew(
	private val tradingStrategy: BitmexTradingStrategy,
	private val internalMap: HashMap<String, BitmexOrder> = hashMapOf()) : Map<String, BitmexOrder> by internalMap {
	
	val graveyard = BitmexOrderGraveyard(20_000)
	
	fun handle(bitmexOrders: BitmexOrders) = bitmexOrders.forEach { handle(it) }
	
	fun handle(bitmexOrder: BitmexOrder) {
		// make sure that order has clOrdID
		if (bitmexOrder.clOrdID == null) {
			if (bitmexOrder.orderID == null) {
				throw RuntimeException("order $bitmexOrder has neither clOrdID nor orderID")
			}
			handle(bitmexOrder.update(BitmexOrder(clOrdID = bitmexOrder.orderID, timestamp = bitmexOrder.timestamp)))
			return
		}
		
		val internalOrder: BitmexOrder? = internalMap[bitmexOrder.clOrdID]
		updatePositionIfOrderWasFilled(bitmexOrder, internalOrder)
		
		if (graveyard.contains(bitmexOrder.clOrdID)) {
			return
		}
		
		
		
		if (bitmexOrder.isDead) {
			remove(bitmexOrder)
			val takerOnTheWay = tradingStrategy.takerOnTheWay
			if (takerOnTheWay != null && bitmexOrder.clOrdID == takerOnTheWay.clOrdID) {
				tradingStrategy.takerOnTheWay = null
			}
			graveyard.add(bitmexOrder.clOrdID)
			return
		}
		
		
		// if order already in map, update it
		if (internalOrder != null) {
			if (bitmexOrder.timestamp >= internalOrder.timestamp) {
				internalMap.remove(internalOrder.clOrdID)
				internalMap.put(internalOrder.clOrdID!!, internalOrder.update(bitmexOrder))
			}
		} else {
			internalMap.put(bitmexOrder.clOrdID, bitmexOrder)
		}
	}
	
	
	fun remove(order: BitmexOrder): BitmexOrder? = internalMap.remove(order.clOrdID)
	
	private fun updatePositionIfOrderWasFilled(bitmexOrder: BitmexOrder, internalOrder: BitmexOrder?) {
		if (internalOrder != null) {
			val toAdjust: Int?
			if (bitmexOrder.cumQty != null) {
				toAdjust = if (internalOrder.cumQty != null) {
					bitmexOrder.cumQty - internalOrder.cumQty
				} else bitmexOrder.cumQty
			} else {
				toAdjust = null
			}
			
			toAdjust?.let {
				if (it <= 0) {
					return
				}
				val toAdjustMaybeNegative = if (internalOrder.side == Side.BUY) toAdjust else -1 * toAdjust
				tradingStrategy.adjustPosition(toAdjustMaybeNegative)
				if (internalOrder.isMaker) {
					addBoughtAndSold(toAdjustMaybeNegative)
				}
			}
		}
	}
	
	private fun addBoughtAndSold(amountMaybeNegative: Int) {
		if (amountMaybeNegative > 0) {
			val newBoughtWithMakers = tradingStrategy.justBoughtWithMakers.addAndGet(amountMaybeNegative)
			println("justBoughtWithMakers is now $newBoughtWithMakers")
		} else {
			val newSoldWithMakers = tradingStrategy.justSoldWithMakers.addAndGet(amountMaybeNegative.abs)
			println("justSoldWithMakers is now $newSoldWithMakers")
		}
	}
	
}*/
