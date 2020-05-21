package tothrosch.util.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tothrosch.networking.rest.RestClient

class TimedAction(val interval: Long, val startingDelay: Long, val restClient: RestClient, val block: () -> Unit) {
	
	var running = false
	
	fun start() {
		restClient.launch {
			delay(startingDelay)
			this@TimedAction.running = true
			while (true) {
				val nowMoment = System.currentTimeMillis()
				val nextMoment = if (nowMoment % interval == 0L) nowMoment else ((nowMoment / interval) * interval + interval)
				delay(nextMoment - nowMoment)
				block()
				delay(15)
			}
		}
	}
}