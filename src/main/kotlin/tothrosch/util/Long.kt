package tothrosch.util

import tothrosch.util.time.getMonthLetter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Instant.toDateCode(): String {
	val localDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)
	val yearString = localDateTime.year.toString().drop(2)
	val monthLetter = getMonthLetter(localDateTime.month)
	var dayString = localDateTime.dayOfMonth.toString()
	val nowLen = dayString.length
	if (nowLen < 2) {
		dayString = "0" + dayString
	}
	return yearString + monthLetter + dayString
}