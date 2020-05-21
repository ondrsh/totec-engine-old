package tothrosch.util.time

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tothrosch.engine.message.Message
import tothrosch.settings.Settings

class LiveTimerPing<T : R, R>(private val itemToSend: T,
                              val channel: Channel<Message<R>>,
                              private val interval: Long,
                              private val startingDelay: Long) {
	
	
	var running = false
	
	fun start() {
		GlobalScope.launch(Settings.appContext) {
			delay(startingDelay)
			this@LiveTimerPing.running = true
			while (true) {
				val nowMoment = System.currentTimeMillis()
				val nextMoment = if (nowMoment % interval == 0L) nowMoment else ((nowMoment / interval) * interval + interval)
				delay(nextMoment - nowMoment)
				channel.send(Message(itemToSend))
				delay(50)
			}
		}
	}
	
}