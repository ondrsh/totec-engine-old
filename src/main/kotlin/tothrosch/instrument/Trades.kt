package tothrosch.instrument

import tothrosch.instrument.book.Sequencable

class Trades(private val trades: List<Trade>, override val sequence: Long = -1) : List<Trade> by trades, Sequencable
