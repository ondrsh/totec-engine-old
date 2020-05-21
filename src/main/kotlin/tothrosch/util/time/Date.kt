package tothrosch.util.time

import java.text.SimpleDateFormat
import java.time.Month
import java.util.*

fun getMonthLetter(month: Month): String = when (month) {
	Month.JANUARY   -> "F"
	Month.FEBRUARY  -> "G"
	Month.MARCH     -> "H"
	Month.APRIL     -> "J"
	Month.MAY       -> "K"
	Month.JUNE      -> "M"
	Month.JULY      -> "N"
	Month.AUGUST    -> "Q"
	Month.SEPTEMBER -> "U"
	Month.OCTOBER   -> "V"
	Month.NOVEMBER  -> "X"
	Month.DECEMBER  -> "Z"
}


object DateFormat {
	private val withMillis = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
	init {
		withMillis.timeZone = TimeZone.getTimeZone("UTC")
	}
	
	fun getUtcString(nowMillis: Long): String {
		val date = Date(nowMillis)
		return withMillis.format(date)
	}
	
}

