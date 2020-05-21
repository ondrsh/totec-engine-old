package tothrosch.trading.orders

import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.TypeFactory
import tothrosch.instrument.Instrument

// TODO ("try to make a list out of this")
open class BitmexOrders(private val orders: List<BitmexOrder>): List<BitmexOrder> by orders {
	
	fun invalidate() = BitmexOrders(this.orders.map { it.update(workingIndicator = false, timestamp = it.timestamp) })
	fun update(newOrders: List<BitmexOrder>) = BitmexOrders(orders = newOrders)
}


fun List<BitmexOrder>.invalidate() =
	this.map { it.update(workingIndicator = false, timestamp = it.timestamp) }

val bitmexOrderTypeReference: CollectionType = TypeFactory.defaultInstance()
	.constructCollectionType(List::class.java, BitmexOrder::class.java)
val bitmexInstrumentTypeReference: CollectionType = TypeFactory.defaultInstance()
	.constructCollectionType(Set::class.java, Instrument.Bitmex::class.java)
