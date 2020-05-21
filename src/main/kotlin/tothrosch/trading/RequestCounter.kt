package tothrosch.trading

import tothrosch.engine.message.OrderEvent


class RequestCounter {
	private var postsSent = 0L
	private var postsReceived = 0L

	private var amendsSent = 0L
	private var amendsReceived = 0L

	private var cancelSent = 0L
	private var cancelReceived = 0L

	private var cancelAllSent = 0L
	private var cancelAllReceived = 0L
	
	private var closePositionSent = 0L
	private var closePositionReceived = 0L


	fun receive(orderEvent: OrderEvent) {
		when (orderEvent) {
			OrderEvent.POST          -> postsReceived++
			OrderEvent.AMEND         -> amendsReceived++
			OrderEvent.CANCEL        -> cancelReceived++
			OrderEvent.CANCELALL     -> cancelAllReceived++
			OrderEvent.CLOSEPOSITION -> closePositionReceived++
		}
	}
	
	fun send(orderEvent: OrderEvent) {
		when (orderEvent) {
			OrderEvent.POST          -> postsSent++
			OrderEvent.AMEND         -> amendsSent++
			OrderEvent.CANCEL        -> cancelSent++
			OrderEvent.CANCELALL     -> cancelAllSent++
			OrderEvent.CLOSEPOSITION -> closePositionSent++
		}
	}

	fun postOnTheWay() = postsSent > postsReceived
	fun amendOnTheWay() = amendsSent > amendsReceived
	fun cancelOnTheWay() = cancelSent > cancelReceived
	fun cancelAllOnTheWay() = cancelAllSent > cancelAllReceived
	fun closePositionOnTheWay() = closePositionSent > closePositionReceived
	
	
}