package tothrosch.instrument.book.operations

import tothrosch.instrument.book.Bookable


class BookOperations(private val ops: List<BookOperation>, override val sequence: Long = -1) :
	List<BookOperation> by ops, Bookable


