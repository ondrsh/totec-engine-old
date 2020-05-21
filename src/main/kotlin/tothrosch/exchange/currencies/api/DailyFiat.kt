package tothrosch.exchange.currencies.api

import tothrosch.exchange.currencies.Currency
import java.time.LocalDate

class DailyFiat(val localDate: LocalDate, val usdRates: Map<Currency, Double>) : Comparable<DailyFiat> {

	override fun compareTo(other: DailyFiat): Int {
		return this.localDate.compareTo(other.localDate)
	}

	override fun toString(): String {
		val ratesString = usdRates.map { (currency, rate) -> "$currency:$rate" }.joinToString(",")
		return "$localDate,$ratesString${System.lineSeparator()}"
	}

	companion object {
		fun fromLine(line: String): DailyFiat {
			val parts = line.split(",")
			val localDate = LocalDate.parse(parts[0])
			if (localDate.toString() == "2017-09-02") {
				println()
			}
			val usdRates: Map<Currency, Double> = parts.drop(1).map {
				val split = it.split(":")
				Currency.valueOf(split[0]) to split[1].toDouble()
			}.toMap()
			return DailyFiat(localDate, usdRates as HashMap<Currency, Double>)
		}
	}
}