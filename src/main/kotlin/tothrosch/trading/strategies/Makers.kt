package tothrosch.trading.strategies

import tothrosch.instrument.book.BookEntry

open class Makers(val bids: ArrayList<BookEntry>, val asks: ArrayList<BookEntry>, var accounted: Boolean = false) {
	fun clear() {
		bids.clear()
		asks.clear()
	}
	
	fun hasMakers() = bids.isNotEmpty() || asks.isNotEmpty()
	
	fun hasBothMakers() = bids.isNotEmpty() && asks.isNotEmpty()
	
	fun hasNoMakers() = !hasMakers()
	
	val bestBuyMaker: BookEntry
		get() = bids.first()
	
	val bestSellMaker: BookEntry
			get() = asks.first()
	
	
}

