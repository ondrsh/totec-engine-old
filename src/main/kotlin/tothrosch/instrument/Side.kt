package tothrosch.instrument

import com.fasterxml.jackson.annotation.JsonProperty
import tothrosch.engine.Keep

@Keep
enum class Side {
	@JsonProperty(value = "Buy")
	BUY {
		override fun toString() = "B"
	},
	
	@JsonProperty(value = "Sell")
	SELL {
		override fun toString() = "S"
	};
	
	val opposite: Side
		get() = if (this == BUY) SELL else BUY
}