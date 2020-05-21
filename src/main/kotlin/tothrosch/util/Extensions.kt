package tothrosch.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectBuilder
import kotlinx.coroutines.selects.select
import tothrosch.engine.message.Message
import tothrosch.instrument.book.BookEntry
import tothrosch.instrument.handlers.MessageHandler
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun List<BookEntry>.isPopulated(minElements: Int = 3) = this.size >= 3

val Channel<*>.hasElements
	get() = this.isEmpty == false

inline fun <T> Channel<T>.quickSend(obj: T) {
	if (this.offer(obj) == false) throw RuntimeException("Tried to offer object to channel, failed. Object was: $obj")
}

fun CoroutineScope.lazyLaunch(block: suspend CoroutineScope.() -> Unit) = launch(start = CoroutineStart.LAZY,
                                                                                 block = block)
@ExperimentalContracts
inline fun <T, R> Collection<T>.letIfNotEmpty(block: (Collection<T>) -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (this.isEmpty()) null else block(this)
}

inline fun <reified K : Enum<K>, V> List<V>.enumMapOf(keys:Array<K>): EnumMap<K, V> {
    val map = EnumMap<K, V>(K::class.java)
    keys.forEachIndexed{i,k-> map[k]=this[i]}
    return map
}
