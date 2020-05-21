package tothrosch.util

import tothrosch.instrument.book.BookEntry


fun List<BookEntry>.furthestBookEntry(amountToMelt: Double): BookEntry? {
	var amountLeft = amountToMelt
	for (entry: BookEntry in this) {
		amountLeft -= entry.amount
		if (amountLeft <= 0)
			return entry
	}
	return null
}

class ConsumeBookException(p0: String?) : IllegalStateException(p0)