package tothrosch.util.time.log

class LogList: ArrayList<Pair<Event, Long>>() {
	init {
		this.add(Event.CREATED to System.currentTimeMillis())
	}
}