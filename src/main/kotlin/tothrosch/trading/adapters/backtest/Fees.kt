package tothrosch.trading.adapters.backtest


class Fees(val makerFees: ArrayList<Double> = arrayListOf(), val takerFees: ArrayList<Double> = arrayListOf()) {

	fun add(fee: Double) {
		if (fee < 0) {
			makerFees.add(fee)
		} else if (fee > 0) {
			takerFees.add(fee)
		}
	}
}