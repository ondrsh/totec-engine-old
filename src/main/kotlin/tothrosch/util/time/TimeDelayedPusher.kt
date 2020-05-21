package tothrosch.util.time

import tothrosch.ml.features.global.FactorRegulator
import java.util.ArrayDeque


class TimeDelayedPusher(val adders: List<FactorRegulator>, val timeDelay: Long, override val time: Time) : TimeScope {
	
	private val queue: ArrayDeque<Pair<Double, Long>> = ArrayDeque<Pair<Double, Long>>()
	
	
	fun putInQueue(toQueue: Pair<Double, Long>) {
		queue.addLast(toQueue)
	}
	
	
	fun push() {
		
		val itemsToPush = queue.takeWhile { it.second.ago > timeDelay }
		itemsToPush.forEach { pushToAdders(it) }
		for (numberOfItems in itemsToPush.indices) {
			queue.removeFirst()
		}
		
		
		/*	var toTake = 0
			val iter = queue.iterator()
			iterloop@while (iter.hasNext()) {
				val next = iter.next()
				if (next.second.ago < timeDelay) {
					return@iterloop
				}
				pushToAdders(next)
				toTake++
			}
	
			var removed = 0
			while (removed < toTake) {
				queue.removeFirst()
				removed++
			}*/
	}
	
	private fun pushToAdders(toPush: Pair<Double, Long>) {
		adders.forEach { it.add(toPush) }
	}
	
}
