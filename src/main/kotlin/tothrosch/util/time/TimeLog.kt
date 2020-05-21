package tothrosch.util.time

class TimeLog(now: Long) {
	
	val created = now
	val timeLog = arrayListOf<Pair<String, Long>>()
	
	fun addEvent(event: String, time: Long) = timeLog.add(event to time)
}