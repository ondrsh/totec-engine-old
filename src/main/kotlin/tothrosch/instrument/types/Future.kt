package tothrosch.instrument.types

interface Future {
	val listedTime: Long
	val settleTime: Long
	var markPrice: Double
	// var lastmarkPriceUpdate: Long
}