package tothrosch.trading.orders

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import tothrosch.engine.message.OrderEvent
import tothrosch.instrument.Side
import tothrosch.json.NullStringDeserializer
import tothrosch.json.TimestampDeserializer
import tothrosch.util.time.TimeScope
import tothrosch.util.uuid
import java.lang.StringBuilder

// IF CHANGING PROPERTIES, DON'T FORGET TO ALSO CHANGE AT
// 1) update()
// 2) amend()
// 3) toString()
// 4) Serializer
class BitmexOrder(
	val price: Double? = null,
	// note that orders with execInst = 'Close', the orderQty gets deleted when putting into JSON
	// we still use it for backtesting, so make sure you handle it correctly when getting back from Bitmex
	val orderQty: Int? = null,
	val leavesQty: Int? = null,
	val cumQty: Int? = null,
	val side: Side? = null,
	@JsonDeserialize(using = NullStringDeserializer::class) val clOrdID: String? = null,
	@JsonDeserialize(using = NullStringDeserializer::class) val origClOrdID: String? = null,
	val symbol: String? = null,
	val ordType: OrderType? = null,
	val timeInForce: TimeInForce? = null,
	val execInst: ExecInst? = null,
	val ordStatus: Status? = null,
	val workingIndicator: Boolean? = null,
	@JsonDeserialize(using = TimestampDeserializer::class) val timestamp: Long,
	@JsonDeserialize(using = NullStringDeserializer::class) val orderID: String? = null) {
	
	
	val isMaker: Boolean
		get() = this.timeInForce == TimeInForce.GOODTILLCANCEL
	val isTaker: Boolean
		get() = this.timeInForce == TimeInForce.IMMEDIATEORCANCEL
	val isDone: Boolean
		get() = when (ordStatus) {
			Status.FILLED,
			Status.CANCELED,
			Status.DONE_FOR_DAY,
			Status.STOPPED,
			Status.REJECTED,
			Status.EXPIRED -> true
			else           -> false
		}
	// TODO check if this is necessary... don't think so because maker has PARTIALLYFILLED
	/*when (isTaker) {
		true  -> when (ordStatus) {
			Status.FILLED,
			Status.CANCELED,
			Status.DONEFORDAY,
			Status.STOPPED,
			Status.REJECTED,
			Status.EXPIRED -> true
			else           -> false
		}
		false -> when (ordStatus) {
			Status.CANCELED,
			Status.DONEFORDAY,
			Status.STOPPED,
			Status.REJECTED,
			Status.EXPIRED -> true
			else           -> false
		}
	}*/
	
	fun updateWith(newOrder: BitmexOrder) = BitmexOrder(price = newOrder.price ?: this.price,
	                                                    orderQty = newOrder.orderQty ?: this.orderQty,
	                                                    leavesQty = newOrder.leavesQty ?: this.leavesQty,
	                                                    cumQty = newOrder.cumQty ?: this.cumQty,
	                                                    side = newOrder.side ?: this.side,
	                                                    clOrdID = newOrder.clOrdID ?: this.clOrdID,
	                                                    origClOrdID = newOrder.origClOrdID ?: this.origClOrdID,
	                                                    symbol = newOrder.symbol ?: this.symbol,
	                                                    ordType = newOrder.ordType ?: this.ordType,
	                                                    timeInForce = newOrder.timeInForce ?: this.timeInForce,
	                                                    execInst = newOrder.execInst ?: this.execInst,
	                                                    ordStatus = newOrder.ordStatus ?: this.ordStatus,
	                                                    workingIndicator = newOrder.workingIndicator ?: this.workingIndicator,
	                                                    timestamp = newOrder.timestamp,
	                                                    orderID = newOrder.orderID ?: this.orderID)
	
	
	fun update(price: Double? = this.price,
	           orderQty: Int? = this.orderQty,
	           leavesQty: Int? = this.leavesQty,
	           cumQty: Int? = this.cumQty,
	           side: Side? = this.side,
	           clOrdID: String? = this.clOrdID,
	           origClOrdID: String? = this.origClOrdID,
	           symbol: String? = this.symbol,
	           ordType: OrderType? = this.ordType,
	           timeInForce: TimeInForce? = this.timeInForce,
	           execInst: ExecInst? = this.execInst,
	           ordStatus: Status? = this.ordStatus,
	           workingIndicator: Boolean? = this.workingIndicator,
	           timestamp: Long = this.timestamp,
	           orderID: String? = this.orderID) = BitmexOrder(price ?: this.price,
	                                                          orderQty ?: this.orderQty,
	                                                          leavesQty ?: this.leavesQty,
	                                                          cumQty ?: this.cumQty,
	                                                          side ?: this.side,
	                                                          clOrdID ?: this.clOrdID,
	                                                          origClOrdID ?: this.origClOrdID,
	                                                          symbol ?: this.symbol,
	                                                          ordType ?: this.ordType,
	                                                          timeInForce ?: this.timeInForce,
	                                                          execInst ?: this.execInst,
	                                                          ordStatus ?: this.ordStatus,
	                                                          workingIndicator ?: this.workingIndicator,
	                                                          timestamp ?: this.timestamp,
	                                                          orderID ?: this.orderID)
	
	fun getRejected() = this.update(workingIndicator = false,
	                                ordStatus = Status.REJECTED,
	                                timestamp = this.timestamp)
	
	
	// here, amount is actually leavesQty
	fun amend(price: Double? = null, amount: Int? = null, now: Long) = update(price = price ?: this.price,
	                                                                          clOrdID = uuid(),
	                                                                          origClOrdID = clOrdID,
	                                                                          orderQty = if (amount != null) amount + (cumQty ?: 0)
	                                                                          else this.orderQty,
	                                                                          workingIndicator = false,
	                                                                          timestamp = now)
	
	
	fun asCanceledPending(now: Long) = this.update(ordStatus = Status.CANCELED,
	                                               workingIndicator = true,
	                                               timestamp = now)
	
	
	companion object {
		fun newMaker(price: Double, orderQty: Int, side: Side, symbol: String, now: Long) = BitmexOrder(
			price = price,
			orderQty = orderQty,
			leavesQty = orderQty,
			side = side,
			clOrdID = uuid(),
			symbol = symbol,
			ordType = OrderType.LIMIT,
			timeInForce = TimeInForce.GOODTILLCANCEL,
			execInst = ExecInst.PARTICIPATEDONOTINITIATE,
			ordStatus = Status.PENDING_NEW,
			workingIndicator = false,
			timestamp = now
		)
		
		fun newTaker(price: Double, orderQty: Int, side: Side, symbol: String, now: Long) = BitmexOrder(
			price = price,
			orderQty = orderQty,
			leavesQty = orderQty,
			side = side,
			clOrdID = uuid(),
			symbol = symbol,
			ordType = OrderType.LIMIT,
			timeInForce = TimeInForce.IMMEDIATEORCANCEL,
			ordStatus = Status.PENDING_NEW,
			workingIndicator = false,
			timestamp = now
		)
		
		// limit close
		fun newCloseTaker(price: Double, side: Side, symbol: String, now: Long) = BitmexOrder(
			price = price,
			side = side,
			clOrdID = uuid(),
			symbol = symbol,
			ordType = OrderType.LIMIT,
			execInst = ExecInst.CLOSE,
			ordStatus = Status.PENDING_NEW,
			timeInForce = TimeInForce.IMMEDIATEORCANCEL,
			workingIndicator = false,
			timestamp = now
		)
		
		// market close
		fun newCloseTaker(side: Side, symbol: String, now: Long) = BitmexOrder(
			side = side,
			clOrdID = uuid(),
			symbol = symbol,
			ordType = OrderType.LIMIT,
			execInst = ExecInst.CLOSE,
			ordStatus = Status.PENDING_NEW,
			workingIndicator = false,
			timestamp = now
		)
		
	}
	
	
	fun collidesWith(newOrder: BitmexOrder) =
		(this.side == Side.SELL && newOrder.side == Side.BUY && this.price!! < newOrder.price!!) ||
				(this.side == Side.BUY && newOrder.side == Side.SELL && this.price!! > newOrder.price!!)
	
	/*
	
		override fun equals(other: Any?): Boolean {
			if (other == null) return false
			if (other !is BitmexOrder) return false
			if (other.clOrdID != null && this.clOrdID != null && other.clOrdID == this.clOrdID) {
				if (this.price == other.price &&
					this.leavesQty == other.leavesQty &&
						this.timestamp == other.timestamp)
				return true
			}
			if (other.orderID != null && this.orderID != null && other.orderID == this.orderID) {
				return true
			}
			return false
		}
	*/
	
	
	override fun hashCode(): Int {
		var result = 17
		result = 31 * result + (clOrdID?.hashCode() ?: orderID!!.hashCode())
		return result
	}
	
	
	override fun toString(): String {
		val stringList: ArrayList<String> = arrayListOf()
		
		price?.let { stringList.add("price: ${it}") }
		orderQty?.let { stringList.add("orderQty: ${it}") }
		leavesQty?.let { stringList.add("leavesQty: ${it}") }
		cumQty?.let { stringList.add("cumQty: ${it}") }
		side?.let { stringList.add("side: ${it}") }
		clOrdID?.let { stringList.add("clOrdID: ${it}") }
		origClOrdID?.let { stringList.add("origClOrdID: ${it}") }
		symbol?.let { stringList.add("symbol: ${it}") }
		ordType?.let { stringList.add("ordType: ${it}") }
		timeInForce?.let { stringList.add("timeInForce: ${it}") }
		execInst?.let { stringList.add("execInst: ${it}") }
		ordStatus?.let { stringList.add("ordStatus: ${it}") }
		workingIndicator.let { stringList.add("workingIndicator: ${it}") }
		timestamp.let { stringList.add("timestamp: ${it}") }
		orderID?.let { stringList.add("orderID: ${it}") }
		
		
		return "{${stringList.joinToString(", ")}}"
		
		
	}
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as BitmexOrder
		
		if (price != other.price) return false
		if (orderQty != other.orderQty) return false
		if (leavesQty != other.leavesQty) return false
		if (cumQty != other.cumQty) return false
		if (side != other.side) return false
		if (clOrdID != other.clOrdID) return false
		if (origClOrdID != other.origClOrdID) return false
		if (symbol != other.symbol) return false
		if (ordType != other.ordType) return false
		if (timeInForce != other.timeInForce) return false
		if (execInst != other.execInst) return false
		if (ordStatus != other.ordStatus) return false
		if (workingIndicator != other.workingIndicator) return false
		if (timestamp != other.timestamp) return false
		if (orderID != other.orderID) return false
		
		return true
	}
	
	
	enum class Status {
		@JsonProperty("New") NEW {
			override val isOpen = true
		},
		@JsonProperty("PartiallyFilled") PARTIALLY_FILLED {
			override val isOpen = true
		},
		@JsonProperty("Filled") FILLED {
			override val isOpen = false
		},
		@JsonProperty("DoneForDay") DONE_FOR_DAY {
			override val isOpen = true
		},
		@JsonProperty("Canceled") CANCELED {
			override val isOpen = false
		},
		@JsonProperty("PendingCancel") PENDING_CANCEL {
			override val isOpen = false
		},
		@JsonProperty("Stopped") STOPPED {
			override val isOpen = false
		},
		@JsonProperty("Rejected") REJECTED {
			override val isOpen = false
		},
		@JsonProperty("PendingNew") PENDING_NEW {
			override val isOpen = false
		},
		@JsonProperty("Expired") EXPIRED {
			override val isOpen = false
		};
		
		abstract val isOpen: Boolean
	}
	
	enum class TimeInForce {
		@JsonProperty("Day") DAY,
		@JsonProperty("GoodTillCancel") GOODTILLCANCEL,
		@JsonProperty("ImmediateOrCancel") IMMEDIATEORCANCEL,
		@JsonProperty("FillOrKill") FILLORKILL
	}
	
	enum class OrderType {
		@JsonProperty("Market") MARKET,
		@JsonProperty("Limit") LIMIT,
		@JsonProperty("MarketWithLeftOverAsLimit") MARKETWITHLEFTOVERASLIMIT,
		@JsonProperty("Stop") STOP,
		@JsonProperty("StopLimit") STOPLIMIT,
		@JsonProperty("MarketIfTouched") MARKETIFTOUCHED,
		@JsonProperty("LimitIfTouched") LIMITIFTOUCHED
	}
	
	enum class ExecInst {
		@JsonProperty("ParticipateDoNotInitiate") PARTICIPATEDONOTINITIATE,
		@JsonProperty("AllOrNone") ALLORNONE,
		@JsonProperty("MarkPrice") MARKPRICE,
		@JsonProperty("IndexPrice") INDEXPRICE,
		@JsonProperty("LastPrice") LASTPRICE,
		
		// CLOSE implies REDUCEONLY
		@JsonProperty("Close") CLOSE,
		@JsonProperty("ReduceOnly") REDUCEONLY
	}
	
	
}

/*val BitmexOrder.isMaker: Boolean
	get() = this.timeInForce == BitmexOrder.TimeInForce.GOODTILLCANCEL
val BitmexOrder.isTaker: Boolean
	get() = this.timeInForce == BitmexOrder.TimeInForce.IMMEDIATEORCANCEL*/
