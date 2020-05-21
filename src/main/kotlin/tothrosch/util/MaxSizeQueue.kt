package tothrosch.util

import tothrosch.ml.features.Resetable
import tothrosch.ml.features.Validatable

interface MaxSizeQueue<T> : Resetable, Validatable {
	
	val first: T
	val last: T
	val size: Int
	
	fun add(element: T)
	//fun addAll(elements: Collection<T>)
	fun clear()
	
	fun copy(): MaxSizeQueue<T>
	fun isEmpty(): Boolean
	fun toList(): List<T>
}