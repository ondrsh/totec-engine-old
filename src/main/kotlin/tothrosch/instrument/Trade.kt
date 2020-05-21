package tothrosch.instrument

import tothrosch.engine.Keep


/**
 * Created by ndrsh on 28.05.17.
 */

// amount is always positive
data class Trade(override val price: Double,
                 val amount: Double,
                 val initiatingSide: Side,
                 override val time: Long = System.currentTimeMillis()) :
	Comparable<Trade>, Ageable, Priceable {
	
	// sort by time, new < old
	override fun compareTo(other: Trade): Int = compareValuesBy(this, other, { it.time })
	
	override fun toString(): String {
		return "$initiatingSide:$price:$amount"
	}
}

