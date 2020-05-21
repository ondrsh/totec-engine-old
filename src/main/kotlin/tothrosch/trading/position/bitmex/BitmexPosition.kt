package tothrosch.trading.position.bitmex

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import tothrosch.engine.Keep
import tothrosch.json.TimestampDeserializer
import tothrosch.trading.position.Position

@Keep
data class BitmexPosition(override val currentQty: Int = 0,
                          val openOrderBuyQty: Int? = null,
                          val openOrderSellQty: Int? = null,
                          override val account: Int? = null,
                          val liquidationPrice: Double? = null,
                          override val avgEntryPrice: Double? = null,
                          @JsonProperty("symbol") val symbol: String? = null,
                          @JsonDeserialize(using = TimestampDeserializer::class) val timestamp: Long) : Position

