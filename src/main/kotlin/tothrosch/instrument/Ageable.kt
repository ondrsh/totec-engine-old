package tothrosch.instrument

// Record time of initiation for objects so you can kill them later when they are too old
interface Ageable {
	val time: Long
}