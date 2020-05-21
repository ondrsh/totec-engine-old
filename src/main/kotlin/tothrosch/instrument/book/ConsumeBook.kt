package tothrosch.instrument.book

/**
 * Created by ndrsh on 12.06.17.
 */

/*
class ConsumeBook(val bids: List<BookEntry> = listOf(), val asks: List<BookEntry> = listOf(), val time: Long = now)   {
    val isConsistent: Boolean
        get() = bids.size >= 1 && asks.size >= 1 && bids.get(0).price <= asks.get(0).price

    override fun toString(): String {
        return "BIDS: ${bids.joinToString(" / ")}, ASKS: ${asks.joinToString(" / ")}"
    }

    fun toDb(): String {
        return "S,${bids.map { it.toDb() }.joinToString("/")}$$$$$${asks.map { it.toDb() }.joinToString("/")}"
    }
}
*/

