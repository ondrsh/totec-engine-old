package tothrosch.instrument.book

import tothrosch.instrument.Side
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.settings.Settings
import tothrosch.util.round
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max


/**
 * Created by ndrsh on 02.06.17.
 */


open class BidsMutable(
	underlyingMap: HashMap<String, BookEntry> = HashMap(),
	underlyingSet: SortedSet<BookEntry> = TreeSet(BidsComparator)) :
	BookSideMutable(underlyingMap, underlyingSet, side = Side.BUY)

open class AsksMutable(
	underlyingMap: HashMap<String, BookEntry> = HashMap(),
	underlyingSet: SortedSet<BookEntry> = TreeSet(AsksComparator)) :
	BookSideMutable(underlyingMap, underlyingSet, side = Side.SELL)

sealed class BookSideMutable(
	val map: HashMap<String, BookEntry>,
	val set: SortedSet<BookEntry>,
	val maxSize: Int = Settings.bookArraySize,
	var list20: List<BookEntry> = set.toList(),
	val side: Side) {
	
	var updateNecessary = false
	
	init {
		if (set.size > maxSize) {
			val toRemove: List<BookEntry> = set.tailSet(set.toList()[maxSize]).toList()
			for (entry: BookEntry in toRemove) {
				removeOrder(entry)
			}
			updateFastOrderList()
		}
	}
	
	// we just get the hypothetical operations, we are not changing any state here
	fun processSnapshot(snapshotMutable: BookSideMutable, snapshotTime: Long): List<BookOperation> {
		val finalOps: MutableList<BookOperation> = arrayListOf()
		val side: Side = if (snapshotMutable is BidsMutable) Side.BUY else Side.SELL
		
		// check if old entries are contained in new snapshot
		// if not, add BookDelete
		for ((internalID, internalEntry) in map) {
			// obviously, my entries cannot be in new snapshot, so skip them
			if (internalEntry.isMine) continue
			if (snapshotMutable.map.containsKey(internalID) == false) {
				finalOps.add(BookOperation.Delete(side = side,
				                                  bookEntry = internalEntry.copy(survivedTime = snapshotTime - internalEntry.time),
				                                  distanceToTop = internalEntry.distance))
			}
		}
		
		// then check if new entries are contained in old snapshot
		val lastEntry = set.last()
		for ((orderID, newEntry) in snapshotMutable.map) {
			val oldEntry: BookEntry? = this.map[orderID]
			// if not, insert
			if (oldEntry == null) {
				if (newEntry.price != 0.0
					&& newEntry.amount != 0.0
					// this has to be checked, because if I have orders inside, then some will be pushed out
					&& set.comparator().compare(newEntry, lastEntry) == -1) {
					finalOps.add(BookOperation.Insert(side, newEntry, newEntry.distance))
				}
				continue
			}
			// if yes, check if price is same?
			else {
				// if no, add Insert+Remove
				if (oldEntry.price != newEntry.price) {
					if (newEntry.price != 0.0) {
						finalOps.add(BookOperation.Delete(side, oldEntry.copy(survivedTime = snapshotTime - oldEntry.time), oldEntry.distance))
						finalOps.add(BookOperation.Insert(side, newEntry, newEntry.distance))
					}
				}
				// if yes --> check if amount same
				else {
					// if no, add Change with diffamount
					val deltaAmount = (oldEntry.amount - newEntry.amount).round(11)
					if (deltaAmount != 0.0) {
						val deltaEntry: BookEntry = oldEntry.copy(amount = deltaAmount,
						                                          time = snapshotTime,
						                                          survivedTime = snapshotTime - oldEntry.time)
						finalOps.add(BookOperation.Change(side, deltaEntry, deltaEntry.distance))
					}
				}
			}
		}
		
		return finalOps
	}
	
	// TODO check when trading live that my orders are take care of... hard
	fun processAllBookOps(ops: List<BookOperation>): List<BookOperation> {
		val finalOps = arrayListOf<BookOperation>()
		for (op in ops) {
			processBookOp(finalOps, op)
		}
		if (finalOps.isNotEmpty()) updateNecessary = true
		return finalOps
	}
	
	
	var debug = 1
	var i = 0
	var c = 0
	var d = 0
	fun processBookOp(finalOps: ArrayList<BookOperation>, op: BookOperation) {
		when (op) {
			is BookOperation.Insert -> {
				processInsert(finalOps, op)
				i++
			}
			is BookOperation.Delete -> {
				processDelete(finalOps, op)
				d++
			}
			is BookOperation.Change -> {
				// processChange2(finalOps, op)
				val oldEntry = map[op.bookEntry.id]
				oldEntry?.changeAmount(finalOps, op)
				c++
			}
			// is BookOperation.Change -> processChange2(finalOps, op)
			// is BookOperation.Trade  -> processTrade(finalOps, op)
		}
		if (debug % 1_000_000 == 0) {
			println("${set.take(10).map { it.amount / it.price }.sum()} \t I $i \t C $c \t D $d")
		}
		debug += 1
	}
	
	fun processInsert(finalOps: ArrayList<BookOperation>, op: BookOperation.Insert) {
		val oldEntry: BookEntry? = map[op.bookEntry.id]
		// if orders is not in bookSide, try adding - if successful, return BookInsert
		// TODO check logs if this ever happens.. don't think so
		if (oldEntry != null && oldEntry.price == 0.0) {
			println("trying to insert, but already order with price 0.0 inside")
		}
		if (oldEntry == null) {
			if (op.bookEntry.price == 0.0 || op.bookEntry.amount == 0.0) {
				return
			}
			if (addOrder((op.bookEntry))) {
				finalOps.add(BookOperation.Insert(op.side, op.bookEntry, op.bookEntry.distance))
			}
		}
		// else if order is already there, create BookChange and return that
		else {
			if (op.bookEntry.price > 0 && op.bookEntry.price != oldEntry.price) {
				println("HAS THAT EVER HAPPENED")
				// price changed then, make delete and insert
				processDelete(finalOps, BookOperation.Delete(op.side, oldEntry))
				val newEntry = op.bookEntry.copy(amount = if (op.bookEntry.amount > 0) op.bookEntry.amount else oldEntry.amount)
				addOrder(newEntry)
				finalOps.add(BookOperation.Insert(side = op.side,
				                                  bookEntry = newEntry,
				                                  distanceToTop = newEntry.distance))
				return
			}
			oldEntry.changeAmountByInsertEntry(finalOps, op)
			/*val deltaEntry: BookEntry = oldEntry.copy(amount = (op.bookEntry.amount - oldEntry.amount).round(11),
			                                          time = op.bookEntry.time)
			if (deltaEntry.amount != 0.0) {
				processChange2(finalOps, BookOperation.Change(side = op.side,
				                                              deltaEntry = deltaEntry)) // distance will be calculated later
			} else {
				// println("delta entry had amount 0.0, not good")
			}*/
		}
	}
	
	private fun processDelete(finalOps: ArrayList<BookOperation>, op: BookOperation.Delete) {
		// if entry is not in Map, return null, because we cannot remove it
		val oldEntry: BookEntry = map.remove(op.bookEntry.id) ?: return // ?: throw MutableBookException("could not remove entry $entry from bookarray ${if (this is BidsMutable) "bidsMutable" else "asksMutable"}")
		set.remove(oldEntry)
		val millisSurvived: Long = op.bookEntry.time - oldEntry.time
		val entryToRemove: BookEntry = oldEntry.copy(survivedTime = millisSurvived)
		finalOps.add(BookOperation.Delete(op.side, entryToRemove, oldEntry.distance))
	}
	
	// two possibilities for a BookEntry:
	// 1) it is not in set - cannot change it, so return null
	// 2) it is in set - create newEntry with amount = oldEntry.amount + deltaEntry.amount, then remove oldEntry and add newEntry
	// then, return the BookChange with deltaEntry (which has survivedTime as age)
	private fun processChange2(finalOps: ArrayList<BookOperation>, op: BookOperation.Change) {
		val oldEntry: BookEntry = map[op.bookEntry.id] ?: return
		val currentTime: Long = op.bookEntry.time
		val millisSurvived: Long = currentTime - oldEntry.time
		// important that you use price from oldEntry... newEntry might not have a price, only ID
		val deltaEntry: BookEntry = op.bookEntry.copy(price = oldEntry.price, time = currentTime, survivedTime = millisSurvived)
		if (deltaEntry.amount == 0.0) {
			println("processed change where deltaEntry had amount 0.0")
			return
		}
		val newEntry: BookEntry = oldEntry.copy(amount = (oldEntry.amount + deltaEntry.amount).round(11), time = currentTime)
		if (newEntry.price == 0.0) {
			println("processed change where newEntry had price 0.0")
		}
		val removalSuccess: Boolean = removeOrder(oldEntry)
		if (newEntry.amount == 0.0) {
			finalOps.add(BookOperation.Delete(op.side, oldEntry.copy(survivedTime = millisSurvived), oldEntry.distance))
			return
		}
		val addingSuccess: Boolean = addOrder(newEntry)
		if (removalSuccess && addingSuccess) {
			finalOps.add(BookOperation.Change(op.side, deltaEntry, deltaEntry.distance))
		} else throw BookSideMutableException("Couldn't process BookChange - oldEntry is in set/map, but could not remove and add new one")
	}
	
	// two possibilities for a BookEntry:
	// 1) it is not in set - cannot change it, so return null
	// 2) it is in set - create newEntry with amount = oldEntry.amount + deltaEntry.amount, then remove oldEntry and add newEntry
	// then, return the BookChange with deltaEntry (which has survivedTime as age)
	private fun processChange(finalOps: ArrayList<BookOperation>, op: BookOperation.Change) {
		val oldEntry: BookEntry = map[op.bookEntry.id] ?: return
		val millisSurvived: Long = op.bookEntry.time - oldEntry.time
		// important that you use price from oldEntry... newEntry might not have a price, only ID
		if (op.bookEntry.amount == 0.0) {
			println("processed change where deltaEntry had amount 0.0")
			return
		}
		op.bookEntry.survivedTime = millisSurvived
		op.bookEntry.price = oldEntry.price
		oldEntry.amount = (oldEntry.amount + op.bookEntry.amount).round(11)
		oldEntry.time = op.bookEntry.time
		if (op.bookEntry.price == 0.0) {
			println("processed change where newEntry had price 0.0")
		}
		// val removalSuccess: Boolean = removeOrder(oldEntry)
		if (oldEntry.amount == 0.0) {
			finalOps.add(BookOperation.Delete(op.side, oldEntry.copy(survivedTime = millisSurvived), oldEntry.distance))
			return
		}
		// val addingSuccess: Boolean = addOrder(newEntry)
		// if (removalSuccess && addingSuccess) {
		finalOps.add(BookOperation.Change(op.side, op.bookEntry, op.bookEntry.distance))
		// } else throw BookSideMutableException("Couldn't process BookChange - oldEntry is in set/map, but could not remove and add new one")
	}
	
	private fun BookEntry.changeAmount(finalOps: ArrayList<BookOperation>,
	                                   changeOp: BookOperation) {
		if (changeOp.bookEntry.amount == 0.0) {
			println("we should never change with changeEntry with newAmount of 0.0")
		} else {
			amount = (amount + changeOp.bookEntry.amount).round(11)
			changeOp.bookEntry.survivedTime = changeOp.bookEntry.time - time
			time = changeOp.bookEntry.time
			finalOps.add(changeOp.apply {
				distanceToTop = changeOp.bookEntry.distance
			})
		}
	}
	
	// here, insertEntry gets converted to a deltaEntry
	private fun BookEntry.changeAmountByInsertEntry(finalOps: ArrayList<BookOperation>,
	                                                insertOp: BookOperation) {
		if (insertOp.bookEntry.amount == 0.0) {
			println("we should never change with insertEntry with newAmount of 0.0")
		} else {
			val diff = (insertOp.bookEntry.amount - amount).round(11)
			amount = insertOp.bookEntry.amount
			insertOp.bookEntry.amount = diff
			
			insertOp.bookEntry.survivedTime = insertOp.bookEntry.time - time
			time = insertOp.bookEntry.time
			// TODO CARE THIS IS WRONG it's not an insert entry
			finalOps.add(insertOp.apply {
				distanceToTop = insertOp.bookEntry.distance
			})
		}
	}
	
	
	// only used for backtesting
	/*fun processTrade(finalOps: ArrayList<BookOperation>, op: BookOperation.Trade) {
		val toMeltAway = mutableSetOf<BookEntry>()
		loop@ for (entry in underlyingSet) {
			if (underlyingSet.comparator().compare(entry, op.bookEntry) < 0) {
				if (op.bookEntry.price != entry.price) {
					toMeltAway.add(entry)
					// processDelete(finalOps, BookOperation.Delete(op.side, entry, entry.distance))
				} else {
					break@loop
				}
			} else {
				break@loop
			}
		}
		toMeltAway.forEach { processDelete(finalOps, BookOperation.Delete(op.side, it, it.distance)) }
	}*/
	
	fun updateFastOrderList() {
		if (updateNecessary) {
			list20 = set.take(20)
			updateNecessary = false
		}
	}
	
	fun addOrder(entry: BookEntry): Boolean {
		// TODO
		if (entry.price == 0.0) {
			println("processed insert where entry had price ${entry.price} and amount ${entry.amount}")
		}
		return if (set.size < maxSize) {
			set.add(entry)
			map[entry.id] = entry
			true
		} else {
			if (set.comparator().compare(entry, set.last()) == -1) {
				set.add(entry)
				map[entry.id] = entry
				removeOrder(set.last())
				return true
			}
			false
		}
	}
	
	
	private fun removeOrder(entry: BookEntry) = set.remove(entry) && map.remove(entry.id) != null
	
	fun hasCorrectSize(): Boolean {
		return (set.size == map.size && set.size > 3)
	}
	
	private val BookEntry.distance: Double
		get() = if (set.size == 0) 0.0 else {
			if (this@BookSideMutable.side == Side.BUY) max(set.first().price - this.price, 0.0) else max(this.price - set.first().price, 0.0)
		}
	
	
}

class BookSideMutableException(p0: String?) : RuntimeException(p0)

// for asks, entry with lowest price is ranked lowest (so it is at position 0 in set and lists)
// for bids, entry with highest price is ranked lowest (so it is at position 0 in set and lists)
sealed class BookEntryComparator : Comparator<BookEntry>

object AsksComparator : BookEntryComparator() {
	override fun compare(p0: BookEntry, p1: BookEntry): Int {
		val priceCompare = p0.price.compareTo(p1.price)
		return if (priceCompare == 0) {
			val timeCompare = p0.time.compareTo(p1.time)
			return if (timeCompare == 0) {
				p0.id.compareTo(p1.id)
			} else {
				timeCompare
			}
		} else {
			priceCompare
		}
	}
}

object BidsComparator : BookEntryComparator() {
	override fun compare(p0: BookEntry, p1: BookEntry): Int {
		val priceCompare = p1.price.compareTo(p0.price)
		return if (priceCompare == 0) {
			val timeCompare = p0.time.compareTo(p1.time)
			return if (timeCompare == 0) {
				p0.id.compareTo(p1.id)
			} else {
				timeCompare
			}
		} else {
			priceCompare
		}
	}
}