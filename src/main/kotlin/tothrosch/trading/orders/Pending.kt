package tothrosch.trading.orders

import tothrosch.engine.Keep
import tothrosch.engine.message.OrderEvent

@Keep
data class Pending(val event: OrderEvent)
