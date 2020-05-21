package tothrosch.engine

import kotlinx.coroutines.delay
import tothrosch.exchange.Exchange
import tothrosch.networking.rest.adapter.bitmex.BitmexRestClient
import tothrosch.util.uuid


object ConnectionTest {
	
	val exchange = Exchange.BitmexExchange
	val restClient: BitmexRestClient = exchange.restClient
	val uuid1 = uuid()
	val uuid2 = uuid()
	val uuid3 = uuid()
	
	suspend fun latency() {
		
		post()
		delay(7000)
		while (true) {
			amend1()
			delay(600)
			amend2()
			delay(600)
		}
		
		
	}
	
	
	suspend fun post() {
		
		/*val returnOrderChannel = Channel<Pair<Message<BitmexOrders>, Request?>>(10)
		val order1 = BitmexOrder(
			price = 12012.0,
			orderQty = 57,
			symbol = "XBTUSD",
			side = Side.BUY,
			clOrdID = uuid1,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val order2 = BitmexOrder(
			price = 12082.0,
			orderQty = 55,
			symbol = "XBTUSD",
			side = Side.BUY,
			clOrdID = uuid2,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val order3 = BitmexOrder(
			price = 19390.0,
			orderQty = 60,
			symbol = "XBTUSD",
			side = Side.SELL,
			clOrdID = uuid3,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		
		val req = OrderRequest(OrderRequest.Type.POST, returnOrderChannel, listOf(order1, order2, order3))
		
		
		restClient.orderRequestHandler.send(req)
		returnOrderChannel.receive()*/
		
	}
	
	suspend fun amend1() {
	/*	val returnOrderChannel = Channel<Pair<Message<BitmexOrders>, OrderRequest?>>(10)
		val order1 = BitmexOrder(
			price = 12003.0,
			leavesQty = 72,
			symbol = "XBTUSD",
			side = Side.BUY,
			clOrdID = uuid1,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			timeInForce = BitmexOrder.TimeInForce.GOODTILLCANCEL,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val order2 = BitmexOrder(
			price = 12173.0,
			leavesQty = 55,
			symbol = "XBTUSD",
			side = Side.BUY,
			clOrdID = uuid2,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			timeInForce = BitmexOrder.TimeInForce.GOODTILLCANCEL,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val order3 = BitmexOrder(
			price = 19793.0,
			leavesQty = 64,
			symbol = "XBTUSD",
			side = Side.SELL,
			clOrdID = uuid3,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			timeInForce = BitmexOrder.TimeInForce.GOODTILLCANCEL,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val req = OrderRequest(OrderRequest.Type.AMEND, returnOrderChannel, listOf(order1, order2, order3))
		
		
		restClient.orderRequestHandler.send(req)
		returnOrderChannel.receive()
		
	*/
	}
	
	suspend fun amend2() {
	/*	val returnOrderChannel = Channel<Pair<Message<BitmexOrders>, OrderRequest?>>(10)
		val order1 = BitmexOrder(
			price = 12006.0,
			leavesQty = 63,
			symbol = "XBTUSD",
			side = Side.BUY,
			clOrdID = uuid1,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			timeInForce = BitmexOrder.TimeInForce.GOODTILLCANCEL,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val order2 = BitmexOrder(
			price = 12171.5,
			leavesQty = 81,
			symbol = "XBTUSD",
			side = Side.BUY,
			clOrdID = uuid2,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			timeInForce = BitmexOrder.TimeInForce.GOODTILLCANCEL,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val order3 = BitmexOrder(
			price = 19773.0,
			leavesQty = 61,
			symbol = "XBTUSD",
			side = Side.SELL,
			clOrdID = uuid3,
			workingIndicator = false,
			ordType = BitmexOrder.OrderType.LIMIT,
			timeInForce = BitmexOrder.TimeInForce.GOODTILLCANCEL,
			execInst = BitmexOrder.ExecInst.PARTICIPATEDONOTINITIATE,
			timestamp = now
		)
		
		val req = OrderRequest(OrderRequest.Type.AMEND, returnOrderChannel, listOf(order1, order2, order3))
		
		
		restClient.orderRequestHandler.send(req)
		returnOrderChannel.receive()*/
	}
	
}