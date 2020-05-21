package tothrosch.util

import tothrosch.util.time.TimeScope
import java.time.Instant

fun TimeScope.log(msg: String = "") {
	println("${Instant.ofEpochMilli(this.time.now)}, $msg")
}

fun logLive(msg: String = "") {
	println("${Instant.now()}, $msg")
}

