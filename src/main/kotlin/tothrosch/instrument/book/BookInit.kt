package tothrosch.instrument.book

import tothrosch.engine.message.Message
import tothrosch.instrument.book.operations.BookOperations
import java.util.*

/**
 * Created by ndrsh on 04.07.17.
 */

class BookInit {
	
	val msgList: TreeSet<Message<BookOperations>> = TreeSet(BookInitComparator)
	var snapshotSequence: Long = Long.MAX_VALUE
	var sending: Boolean = false
	var initialized: Boolean = false
	var oldBookCount: Int = 0
	
	fun removeEarlyMessages(): Boolean = msgList.removeIf { it.content.sequence <= snapshotSequence }
	
	// only when reconnecting
	fun reset() {
		sending = false
		snapshotSequence = Long.MAX_VALUE
		msgList.clear()
		oldBookCount = 0
		initialized = false
	}
	
	
	object BookInitComparator : Comparator<Message<BookOperations>> {
		override fun compare(p0: Message<BookOperations>?, p1: Message<BookOperations>?): Int {
			return p0!!.content.sequence.compareTo(p1!!.content.sequence)
		}
	}
	
}