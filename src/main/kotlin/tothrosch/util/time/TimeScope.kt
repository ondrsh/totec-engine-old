package tothrosch.util.time

import tothrosch.engine.Keep

@Keep
interface TimeScope {
	val time: Time
	
	val now: Long
		get() = time.now
	
	val Long.ago: Long
		get() = time.now - this
}



