package tothrosch.instrument

import com.fasterxml.jackson.databind.node.ObjectNode


typealias Update = ObjectNode
	
	/*fun toDb(): String {
		val finalString = StringBuilder()
		
		var addedFirst = false
		for (entry in this) {
			if (addedFirst) {
				finalString.append("/")
			}
			finalString.append("${entry.key}:${entry.value}")
			addedFirst = true
		}
		return finalString.toString()
	}*/


/*companion object {
	fun createFromSplitString(instrument: Instrument, splitString: List<String>) = when (splitString[0]) {
		"BS"    -> createSwapUpdateFromString(instrument as Instrument.Bitmex.Swap, splitString[1])
		"BF"    -> createFutureUpdateFromString(instrument as Instrument.Bitmex.Future, splitString[1])
		else    -> throw ClassNotFoundException("could not find Update class from string ${splitString[0]}")
	}
	
	fun createSwapUpdateFromString(swap: Instrument.Bitmex.Swap, string: String): Update.Bitmex.Swap {
		val pairsList = string.split("/").map { it.split(":") }
		var markPrice: Double? = null
		var openInterest: Int? = null
		var fundingRate: Double? = null
		var indicativeFundingRate: Double? = null
		for (pair in pairsList) {
			when (pair[0]) {
				"mp"  -> markPrice = pair[1].toDouble()
				"oi"  -> openInterest = pair[1].toInt()
				"fr"  -> fundingRate = pair[1].toDouble()
				"ifr" -> indicativeFundingRate = pair[1].toDouble()
			}
		}
		return Bitmex.Swap(instrument = swap,
						   markPrice = markPrice,
						   openInterest = openInterest,
						   fundingRate = fundingRate,
						   indicativeFundingRate = indicativeFundingRate)
	}
	
	fun createFutureUpdateFromString(future: Instrument.Bitmex.Future, string: String): Update.Bitmex.Future {
		val pairsList = string.split("/").map { it.split(":") }
		var markPrice: Double? = null
		var openInterest: Int? = null
		var indicativeSettlePrice: Double? = null
		for (pair in pairsList) {
			when (pair[0]) {
				"mp"  -> markPrice = pair[1].toDouble()
				"oi"  -> openInterest = pair[1].toInt()
				"isp" -> indicativeSettlePrice = pair[1].toDouble()
			}
		}
		return Bitmex.Future(instrument = future,
							 markPrice = markPrice,
							 openInterest = openInterest,
							 indicativeSettlePrice = indicativeSettlePrice)
	}
}


// markPrice: Double
// openInterest: Int
sealed class Bitmex(open val instrument: Instrument.Bitmex,
					val markPrice: Double? = null,
					// val lastMarkPriceUpdate: Long? = null,
					val openInterest: Int? = null) : Update(instrument) {
	
	
	override fun perform() {
		if (markPrice != null) instrument.markPrice = markPrice
		// if (lastMarkPriceUpdate != null) instrument.lastMarkPriceUpdate = lastMarkPriceUpdate
		if (openInterest != null) instrument.openInterest = openInterest
	}
	
	override fun createStringMap(): HashMap<String, String> {
		val stringMap = hashMapOf<String, String>()
		if (markPrice != null) stringMap.put("mp", "$markPrice")
		// if (lastMarkPriceUpdate != null) stringMap.put("lmp", "$lastMarkPriceUpdate")
		if (openInterest != null) stringMap.put("oi", "$openInterest")
		return stringMap
	}
	
	class Swap(override val instrument: Instrument.Bitmex.Swap,
			   markPrice: Double? = null,
			   // lastMarkPriceUpdate: Long? = null,
			   openInterest: Int? = null,
			   val fundingRate: Double? = null,
			   val indicativeFundingRate: Double? = null) : Update.Bitmex(instrument, markPrice, openInterest) {
		
		override fun perform() {
			super.perform()
			if (fundingRate != null) instrument.fundingRate = fundingRate
			if (indicativeFundingRate != null) instrument.indicativeFundingRate = indicativeFundingRate
		}
		
		override fun createStringMap(): HashMap<String, String> {
			val stringMap = super.createStringMap()
			if (fundingRate != null) stringMap.put("fr", "$fundingRate")
			if (indicativeFundingRate != null) stringMap.put("ifr", "$indicativeFundingRate")
			return stringMap
		}
	
	}
	
	class Future(override val instrument: Instrument.Bitmex.Future,
				 markPrice: Double? = null,
				 // lastMarkPriceUpdate: Long? = null,
				 openInterest: Int? = null,
				 val indicativeSettlePrice: Double? = null) : Update.Bitmex(instrument, markPrice, openInterest) {
		
		
		override fun perform() {
			super.perform()
			if (indicativeSettlePrice != null) instrument.indicativeSettlePrice = indicativeSettlePrice
		}
		
		override fun createStringMap(): HashMap<String, String> {
			val stringMap = super.createStringMap()
			if (indicativeSettlePrice != null) stringMap.put("isp", "$indicativeSettlePrice")
			return stringMap
		}
	}
}*/
