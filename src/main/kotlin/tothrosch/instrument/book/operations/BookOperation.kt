package tothrosch.instrument.book.operations

import tothrosch.instrument.Side
import tothrosch.instrument.book.BookEntry
import tothrosch.util.compactId


sealed class BookOperation(val side: Side, val bookEntry: BookEntry, var distanceToTop: Double) {
	
	class Insert(side: Side, bookEntry: BookEntry, distanceToTop: Double = -1.0) :
		BookOperation(side, bookEntry, distanceToTop) {
		override fun toString(): String {
			return "$side:${bookEntry.id.compactId()}" +
					":${bookEntry.price}" +
					":${bookEntry.amount}" +
					if (bookEntry.isLiquidation) getLiqAddString() else ""
		}
	}
	
	class Delete(side: Side, bookEntry: BookEntry, distanceToTop: Double = -1.0) :
		BookOperation(side, bookEntry, distanceToTop) {
		override fun toString(): String {
			return "$side" +
					":${bookEntry.id.compactId()}" +
					if (bookEntry.isLiquidation) getLiqAddString() else ""
		}
	}
	
	// here, deltaEntry is not a real entry, it's just the difference
	// if an order gets reduced 100 contracts, deltaentry has amount == -100
	class Change(side: Side, deltaEntry: BookEntry, distanceToTop: Double = -1.0) :
		BookOperation(side, deltaEntry, distanceToTop) {
		override fun toString(): String {
			return "$side:${bookEntry.id.compactId()}" +
					":${bookEntry.amount}" +
					if (bookEntry.isLiquidation) getLiqAddString() else ""
		}
	}
	
	// this is used to mark liquidation bookentries. that way the stringSplit in
	// InstrumentReader has length 5 (overall 4 times ":" means 5 members in split list)
	// that way its compatible to old data
	fun getLiqAddString() = ":::"
	
	// CARE
	// This is only needed to make sure orderbook is half-correct when trades are not received for whatever reason.
	// This is used to artificially generate trades so bids and asks don't overlap
	// you have to provide price for the bookEntry, ID alone is not enough because booksidemutable does not pull from hashmap
	/*class Trade(side: Side, bookEntry: BookEntry, distanceToTop: Double = -1.0) :
		BookOperation(side, bookEntry, distanceToTop) {
		override fun toString(): String {
			return "EXCEPTION"
			//return "$initiatingSide:$price:$amount"
		}
	}*/
}

