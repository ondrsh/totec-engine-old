package tothrosch.trading.position

// care, this class is not immutable, stays the same and changes values
class BacktestPosition(accountStartingValue: Double) : Position {
	
	override var account = accountStartingValue
	override var avgEntryPrice = 0.0
	override var currentQty = 0
	
	var highestPosition = 0
	var maxAccount = account
	var maxLoss = 0.0
}