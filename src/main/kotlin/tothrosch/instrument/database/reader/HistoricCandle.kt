package tothrosch.instrument.database.reader

import tothrosch.instrument.Side
import tothrosch.instrument.Trades

class HistoricCandle(val start: Long, val end: Long, tradesList: List<Trades>) {
	
	val totalBought: Double
	val totalSold: Double
	val net: Double
	val total: Double
	val vwap: Double
	
	init {
		// set amounts
		var bought: Double = 0.0
		var sold: Double = 0.0
		tradesList.forEach {
			it.forEach {
				when (it.initiatingSide) {
					Side.BUY -> bought += it.amount
					Side.SELL -> sold += it.amount
				}}
		}
		totalBought = bought
		totalSold = sold
		net = bought - sold
		total = bought + sold
		
		// set variance
		
		vwap = tradesList.map { it.map { it.price * it.amount } }.flatten().average() / total
	}
	
}
