package tothrosch.util


val Double.sign: Double
	get() = Math.signum(this)

val Double.sqrt: Double
	get() = Math.sqrt(this)

val Double.abs: Double
	get() = Math.abs(this)

fun Double.isNaNorZero(): Boolean = this.isNaN() || this == 0.0

fun Double.round(places: Int): Double {
	val factor: Double = Math.pow(10.0, places * 1.0)
	return Math.round(this * factor) / factor
}

fun Double.format(digits: Int): String {
	val formatted = StringBuilder()
	if (this > 0) {
		formatted.append(" ")
	}
	formatted.append(java.lang.String.format("%.${digits}f", this)!!)
	return formatted.toString()
}

fun Double.format(preDigits: Int, postDigits: Int): String {
	val nowLen = this.format(0).length
	if (nowLen < preDigits) {
		val prefixZeros = StringBuilder()
		for (i in 1..(preDigits - nowLen)) {
			prefixZeros.append("0")
		}
		return prefixZeros.toString() + this.format(postDigits)
	} else {
		return this.format(postDigits)
	}
	
}