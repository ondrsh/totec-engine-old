package tothrosch.instrument.database.reader

import tothrosch.settings.Settings
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ReadSynchronizer() {
	// current millisecond to process
	val currentMs = AtomicLong()
	
	// readers that read the current ms
	private val readersRead = AtomicInteger(0)
	
	// active readers in general
	val activeReaders = AtomicLong()
	
	
	
	
	
	// increments until it reaches max, then sets counter zero
	fun increment() {
		while (true) {
			val existing = readersRead.get()
			val newValue = existing + 1
			if (readersRead.compareAndSet(existing, newValue)) {
				if (newValue >= activeReaders.get()) {
					readersRead.set(0)
					nextMs()
				}
				return;
			}
		}
	}
	
	// no loop here --> if it fails, someone else reset it
	private fun nextMs() {
		while (true) {
			val existing = currentMs.get()
			val newValue = existing + 1
			if (currentMs.compareAndSet(existing, newValue)) {
				return;
			}
		}
	}
}

