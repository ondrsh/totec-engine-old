package tothrosch.instrument.book

import tothrosch.instrument.Priceable
import tothrosch.util.compactId

/**
 * Created by ndrsh on 25.06.17.
*/

data class BookEntry(override var price: Double,
                     var amount: Double,
                     var id: String,
                     var time: Long = System.currentTimeMillis(),
                     var survivedTime: Long = -1,
                     var isLiquidation: Boolean = false,
                     var isMine: Boolean = false) :
	Priceable {
	
	override fun equals(other: Any?): Boolean {
		if (other !is BookEntry) return false
		return this.id == other.id
	}
	
	override fun hashCode(): Int {
		return id.hashCode()
	}
	
	override fun toString(): String = "ID: ${id.compactId()}, PRICE: $price, AMOUNT: $amount"
	
	fun toDb(): String = "${id.compactId()}:$price:$amount${if (isLiquidation) ":Y" else ""}"
}
