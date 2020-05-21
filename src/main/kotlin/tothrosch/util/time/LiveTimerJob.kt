package tothrosch.util.time

import kotlinx.coroutines.*
import tothrosch.settings.Settings
import kotlin.coroutines.CoroutineContext

class LiveTimerJob(private val interval: Long,
                   private val startingDelay: Long,
                   override val coroutineContext: CoroutineContext,
                   private val block: suspend () -> Unit) : CoroutineScope {
	
	
	val job = launch(start = CoroutineStart.LAZY) {
			delay(startingDelay)
			while (true) {
				val nowMoment = System.currentTimeMillis()
				val nextMoment = if (nowMoment % interval == 0L) nowMoment else ((nowMoment / interval) * interval + interval)
				delay(nextMoment - nowMoment)
				block.invoke()
				delay(50)
			}
		}
}

