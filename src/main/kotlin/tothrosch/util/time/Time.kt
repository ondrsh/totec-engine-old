package tothrosch.util.time

sealed class Time {
	abstract val now: Long
	
	abstract fun update(newTime: Long)
	
	object Live : Time() {
		override val now: Long
			get() = System.currentTimeMillis()
		
		override fun update(newTime: Long) {
		}
	}
	
	class Read : Time() {
		@Volatile
		override var now: Long = -1L
			private set
		
		override fun update(newTime: Long) {
			this.now = newTime
		}
	}
}
/*
object GlobalTime: Time() {
	override val now: Long
		get() = System.currentTimeMillis()
			
	override fun update(newTime: Long) {
	}
}*/

/*val Long.ago: Long
	get() = globalTime.now - this

val now: Long
	get() = globalTime.now*/

val seconds = 1000L
val minutes = 60 * seconds
val hours = 60 * minutes

