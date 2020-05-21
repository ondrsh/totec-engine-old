package tothrosch.util.time.log

interface EventLog {
	val log: LogList?
	fun addEvent(event: Event) = log?.add(event to System.currentTimeMillis())
}