package tothrosch.engine.message

import kotlinx.coroutines.channels.Channel
import tothrosch.networking.ConnectionType
import tothrosch.trading.orders.BitmexOrders
import tothrosch.util.time.log.LogList
import tothrosch.engine.message.OrderEvent.*
import tothrosch.trading.Hub
import tothrosch.trading.orders.BitmexOrder

/**
 * Created by ndrsh on 02.07.17.
 */

open class Request<T>(content: T,
                      val returnChannel: Channel<in Request<T>>,
                      error: Error? = null,
                      type: ConnectionType = ConnectionType.REST,
                      timeStamp: Long = System.currentTimeMillis(),
                      log: LogList = LogList()) : Message<T>(content,
                                                             error,
                                                             type,
                                                             timeStamp,
                                                             log)


@Suppress("UNCHECKED_CAST")
class OrderRequest(content: BitmexOrders,
                   val orderEvent: OrderEvent,
                   returnChannel: Channel<in Request<BitmexOrders>> = Hub.orderHandler,
                   error: Error? = null,
                   type: ConnectionType = ConnectionType.REST,
                   timeStamp: Long = System.currentTimeMillis(),
                   log: LogList = LogList()) : Request<BitmexOrders>(content,
                                                                     returnChannel as Channel<in Message<BitmexOrders>>,
                                                                     error,
                                                                     type,
                                                                     timeStamp,
                                                                     log), List<BitmexOrder> by content {
    
    fun updateOrders(newOrders: List<BitmexOrder>) = content.update(newOrders)
}

fun BitmexOrders.getAmendRequest() = OrderRequest(this, AMEND)
fun BitmexOrders.getPostRequest() = OrderRequest(this, POST)
fun BitmexOrder.getPostSingleRequest() = OrderRequest(BitmexOrders(listOf(this)), POST_SINGLE)
fun BitmexOrders.getCancelRequest() = OrderRequest(this, CANCEL)
fun BitmexOrders.getCancelAllRequest() = OrderRequest(this, CANCELALL)
fun BitmexOrder.getClosePositionRequest() = OrderRequest(BitmexOrders(listOf(this)), CLOSEPOSITION)

