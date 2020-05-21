package tothrosch.networking

/**
 * Created by ndrsh on 22.06.17.
 */

class Heartbeat(var time: Long = System.currentTimeMillis()) {
	fun update() {
		time = System.currentTimeMillis()
	}

	fun age(): Long = System.currentTimeMillis() - time
}