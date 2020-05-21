package tothrosch.instrument.book

import tothrosch.instrument.Ageable
import tothrosch.instrument.Side
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.trading.adapters.backtest.Backtest
import tothrosch.util.furthestBookEntry
import tothrosch.util.isPopulated


class Book(var bidsMutable: BidsMutable = BidsMutable(),
           var asksMutable: AsksMutable = AsksMutable(),
           override val sequence: Long = -1L,
           override var time: Long = System.currentTimeMillis()) : Bookable, Ageable {
	
	// two kinds of immutables, the full one and the small one (with 20 orders only)
	var immutableFull = Immutable(bidsMutable.set.toList(), asksMutable.set.toList())
	var immutable20 = immutableFull
	
	operator fun get(side: Side) = if (side == Side.BUY) bidsMutable else asksMutable
	
	fun updateImmutable20() {
		bidsMutable.updateFastOrderList()
		asksMutable.updateFastOrderList()
		immutable20 = Immutable(bidsMutable.list20, asksMutable.list20)
	}
	
	fun isPopulated() = bidsMutable.set.isNotEmpty() && asksMutable.set.isNotEmpty()
	
	fun processBook(newBook: Book, isTrading: Boolean): BookOperations? {
		val finalOps: MutableList<BookOperation> = arrayListOf()
		lateinit var myBids: List<BookEntry>
		lateinit var myAsks: List<BookEntry>
		if (isTrading) {
			myBids = bidsMutable.set.filter { it.isMine }
			myAsks = asksMutable.set.filter { it.isMine }
		}
		val finalBidsOps = bidsMutable.processSnapshot(newBook.bidsMutable, newBook.time)
		val finalAsksOps = asksMutable.processSnapshot(newBook.asksMutable, newBook.time)
		finalOps.addAll(finalBidsOps) //.filter { it.bookEntry.isMine == false })
		finalOps.addAll(finalAsksOps) //.filter { it.bookEntry.isMine == false })
		newBook.bidsMutable.debug = bidsMutable.debug
		newBook.asksMutable.debug = asksMutable.debug
		bidsMutable = newBook.bidsMutable
		asksMutable = newBook.asksMutable
		if (isTrading) {
			// if there was a large time gap, cancel all my orders
			if (newBook.time - this.time > 10_000) Backtest.handleAllCanceledFromBook()
			// else, insert them again (remember we swapped the mutables, so mine are not inside anymore)
			else {
				bidsMutable.processAllBookOps(myBids.map { BookOperation.Insert(Side.BUY, it) })
				asksMutable.processAllBookOps(myAsks.map { BookOperation.Insert(Side.SELL, it) })
			}
		}
		bidsMutable.updateNecessary = true
		asksMutable.updateNecessary = true
		// TODO("check here if the above are the same")
		return if (finalOps.isEmpty()) {
			null
		} else {
			BookOperations(finalOps, sequence = newBook.sequence)
		}
	}
	
	fun processBookOperations(ops: BookOperations): BookOperations? {
		val finalOps: ArrayList<BookOperation> = arrayListOf()
		ops.forEach {
			if (it.side == Side.BUY) bidsMutable.processBookOp(finalOps, it)
			else asksMutable.processBookOp(finalOps, it)
		}
		return if (finalOps.isEmpty()) {
			null
		} else {
			BookOperations(finalOps, sequence = ops.sequence)
		}
	}
	
	
	fun isConsistent(): Boolean = bidsMutable.hasCorrectSize()
				&& asksMutable.hasCorrectSize()
				&& (bidsMutable.set.first().price < asksMutable.set.first().price)
	
	
	// TODO("TEST THIS") this is different now because writer uses this instead of list
	// TODO TEST this
	fun toDb(): String {
		return "BIDS: ${bidsMutable.set.joinToString(" / ")}, ASKS: ${asksMutable.set.joinToString(" / ")}"
	}
	
	
	
	// only top 50 orders for trading instrument
	class Immutable (val bids: List<BookEntry> = listOf(),
	                 val asks: List<BookEntry> = listOf(),
	                 override val time: Long = System.currentTimeMillis()) : Ageable {
		
		val isPopulated
			get() = bids.isNotEmpty() && asks.isNotEmpty()
		
		
		override fun toString(): String {
			return "BIDS: ${bids.joinToString(" / ")}, ASKS: ${asks.joinToString(" / ")}"
		}
		
		/*fun toDb(): String {
			return "S,${bids.joinToString("/") { it.toDb() }}$$$$$${asks.joinToString("/") { it.toDb() }}"
		}*/
		
		fun weightedSpread(size: Double): Double? {
			return (asks.furthestBookEntry(size)?.price ?: return null) - (bids.furthestBookEntry(size)?.price ?: return null)
		}
		
		fun weightedSpreadSide(size: Double, side: Side): Double? {
			when (side) {
				Side.BUY  -> {
					return ((asks.furthestBookEntry(size)?.price ?: return null) - bids[0].price)
				}
				Side.SELL -> {
					return (asks[0].price - (bids.furthestBookEntry(size)?.price ?: return null))
				}
			}
		}
		
		fun isConsistent(): Boolean = bids.isPopulated()
				&& asks.isPopulated()
				&& bids.first().price <= asks.first().price
	}
	
}
