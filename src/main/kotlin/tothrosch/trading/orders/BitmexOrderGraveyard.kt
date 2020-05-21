package tothrosch.trading.orders

import tothrosch.util.decay.AgeableString
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import java.util.*


// temporary place for DEAD orders that linger around for maxAge --> usually around 20 seconds
// this is needed so we don't re-enter dead orders that enter the system delayed via websocket
class BitmexOrderGraveyard(private val maxAge: Long, override val time: Time, private val innerDeque: ArrayDeque<AgeableString> = ArrayDeque(), private val idSet: HashSet<String> = hashSetOf()) : TimeScope {

	fun contains(id: String) = idSet.contains(id)

	fun add(id: String) {
		innerDeque.addLast(AgeableString(id, now))
		innerDeque.addLast(AgeableString(id, now))
		idSet.add(id)
	}

	fun removeOld() {
		while (innerDeque.size > 0 && innerDeque.first.time.ago > maxAge) {
			val tooOldId = innerDeque.removeFirst()
			idSet.remove(tooOldId.string)
		}
	}
}