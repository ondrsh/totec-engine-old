package tothrosch.util

fun String.compactId(): String {
	val parts: List<String> = this.split("-")
	return if (parts.size == 5) parts[4] + parts[1]
	else this
}