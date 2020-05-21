package tothrosch.instrument.candle

import tothrosch.engine.message.Message
import tothrosch.instrument.Instrument
import tothrosch.instrument.Side
import tothrosch.instrument.Trades
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.ml.features.queues.EmaMaxSizeQueue
import tothrosch.ml.features.queues.ReturnEmaMaxSizeQueues
import tothrosch.util.*
import tothrosch.util.time.TimeScope
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

// have to extend "createFreshClone()"
open class QueueContainer(val instrument: Instrument,
                          val bidAskMid: ReturnEmaMaxSizeQueues = ReturnEmaMaxSizeQueues(),
                          val bidAskMid_Weighted: ReturnEmaMaxSizeQueues = ReturnEmaMaxSizeQueues(),
                          val totalTradedInUsd: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val totalBoughtInUsd: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val totalSoldInUsd: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val totalNetTradedInUsd: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val bidAmountAdded: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val bidAmountRemoved: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val bidsAdded: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val bidsRemoved: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val askAmountAdded: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val askAmountRemoved: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val asksAdded: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val asksRemoved: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val averageBidsSurvivedTime: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val averageAsksSurvivedTime: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val weightedSpread: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val averageBidSize: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val averageAskSize: EmaMaxSizeQueue = EmaMaxSizeQueue(),
                          val timeSinceLastCandle: EmaMaxSizeQueue = EmaMaxSizeQueue()) : ArrayList<EmaMaxSizeQueue>(), TimeScope by instrument {
	
	// TODO check if this actually gets read somewhere
	var hasValidIndicators: Boolean = false
	
	init {
		val memberProps = this::class.memberProperties
        this::class.primaryConstructor?.valueParameters?.filter { it.type == EmaMaxSizeQueue::class.createType() }?.forEach { property ->
            add(memberProps.find { it.name == property.name }!!.call(this) as EmaMaxSizeQueue)
        }
    }

	// for each queue, add the corresponding element
    fun addCandle(candle: Candle<Double>): Boolean {
		return if (candle.isValid == false) {
			log("Invalid candle: $candle")
			resetAll()
			hasValidIndicators = false
			false
		} else {
			candle.elements.forEachIndexed { index, element ->
				this[index].add(element)
			}
			hasValidIndicators = areValid()
			true
		}
    }
	
	
	fun addIndicators(opsMsgs: ArrayDeque<Message<BookOperations>>, tradesMsgs: ArrayDeque<Message<Trades>>, usdFactor: Double): Boolean {
		var success = true
		success = success && addBookInfo(usdFactor)
		// success = success && addOps(ops, trades)
		// success = success && addTrades()
		success = success && addTotalTraded(tradesMsgs, usdFactor)
		success = success && addOpsStats(opsMsgs)
		
		if (success == false) {
			resetAll()
		}
		hasValidIndicators = areValid()
		return success
	}
	
	
	private fun addBookInfo(usdFactor: Double): Boolean {
		val bids = instrument.book.bidsMutable.set.take(50)
		val asks = instrument.book.asksMutable.set.take(50)
		if (bids.size < 6 || asks.size < 6) return false
		if (false == bidAskMid.add((asks.first().price + bids.first().price) / 2.0)) return false
		
		val best5Bids = bids.take(5)
		val best5BidsTotalSum = best5Bids.map { it.amount }.sum()
		val best5Asks = asks.take(5)
		val best5AsksTotalSum = best5Asks.map { it.amount }.sum()
		val weightedBid = best5Bids.map { it.amount * it.price }.sum() / best5BidsTotalSum
		val weightedAsk = best5Asks.map { it.amount * it.price }.sum() / best5AsksTotalSum
		val bidAskMidWeighted = (weightedBid + weightedAsk) / 2.0
		if (false == bidAskMid_Weighted.add(bidAskMidWeighted)) return false
		
		if (false == weightedSpread.add(max((weightedAsk - weightedBid) / bidAskMidWeighted, 0.0))) return false
		averageBidSize.add(bids.take(10).map { it.amount }.average())
		averageAskSize.add(asks.take(10).map { it.amount }.average())
		return true
	}
	
	private fun addTotalTraded(trades: ArrayDeque<Message<Trades>>, usdFactor: Double): Boolean {
		// think about why this is necessary
		// if (instrument.pair.base == Currency.BTC && instrument.pair.quote.isFiat) {
		var bought = 0.0
		var sold = 0.0
		trades.forEach {
			it.content.forEach {
				if (it.initiatingSide == Side.BUY) {
					bought += it.amount * if (this is BitmexQueues) 1.0 else it.price * usdFactor
				} else {
					sold += it.amount * if (this is BitmexQueues) 1.0 else it.price * usdFactor
				}
			}
		}
		totalTradedInUsd.add(bought + sold)
		totalBoughtInUsd.add(bought)
		totalSoldInUsd.add(sold)
		totalNetTradedInUsd.add(bought - sold)
		return true
	}
	
	private fun addOpsStats(opsMsgs: ArrayDeque<Message<BookOperations>>): Boolean {
		
		var newBidAmountAdded = 0.0
		var newBidAmountRemoved = 0.0
		var newBidsAdded = 0
		var newBidsRemoved = 0
		
		var newAskAmountAdded = 0.0
		var newAskAmountRemoved = 0.0
		var newAsksAdded = 0
		var newAsksRemoved = 0
		
		val newBidsSurvivedTime = mutableListOf<Long>()
		val newAsksSurvivedTime = mutableListOf<Long>()
		
		for (msg in opsMsgs) {
			for (it in msg.content) {
				
				// check for weird shit for debugging purposes
				if ( it.bookEntry.isMine ||
					it.bookEntry.price <= 0.0 ||
					it.bookEntry.amount.isNaN() ||
					it.bookEntry.price.isNaN() ||
					it.distanceToTop < 0 ||
					(it.netAmount == 0.0 && it !is BookOperation.Delete)
				) {
					println("ERROR OIDA, des bookentry is im oasch ")
					println(it.bookEntry.toString() + " distance to top: ${it.distanceToTop}")
				}
				
				
				val netAmount = it.netAmount
				if (it.side == Side.BUY) {
					// Buy Side
					// distance multi is divided by current price
					val distanceMulti = 1.0 / (1 + it.distanceToTop / (it.bookEntry.price + it.distanceToTop)).pow(600.0)
					if (netAmount > 0) {
						newBidAmountAdded += netAmount * distanceMulti
						newBidsAdded += 1
					} else {
						newBidAmountRemoved += abs(netAmount) * distanceMulti
						newBidsRemoved += 1
						newBidsSurvivedTime.add(max(it.bookEntry.survivedTime, 0))
					}
				} else {
					// Sell side
					val distanceMulti = 1.0 / (1 + it.distanceToTop / (it.bookEntry.price - it.distanceToTop)).pow(600.0)
					if (netAmount > 0) {
						newAskAmountAdded += netAmount * distanceMulti
						newAsksAdded += 1
					} else {
						newAskAmountRemoved += Math.abs(netAmount) * distanceMulti
						newAsksRemoved += 1
						newAsksSurvivedTime.add(Math.max(it.bookEntry.survivedTime, 0))
					}
				}
			}
		}
		
		bidAmountAdded.add(newBidAmountAdded)
		bidAmountRemoved.add(newBidAmountRemoved)
		bidsAdded.add(newBidsAdded.toDouble())
		bidsRemoved.add(newBidsRemoved.toDouble())
		
		askAmountAdded.add(newAskAmountAdded)
		askAmountRemoved.add(newAskAmountRemoved)
		asksAdded.add(newAsksAdded.toDouble())
		asksRemoved.add(newAsksRemoved.toDouble())
		
		averageBidsSurvivedTime.add(
			if (newBidsSurvivedTime.isEmpty()) {
				if (averageBidsSurvivedTime.isEmpty()) {
					return false
				} else {
					averageBidsSurvivedTime.emaLast
				}
			} else {
				newBidsSurvivedTime.average()
			}
		)
	
		averageAsksSurvivedTime.add(
			if (newAsksSurvivedTime.isEmpty()) {
				if (averageAsksSurvivedTime.isEmpty()) {
					return false
				} else {
					averageAsksSurvivedTime.emaLast
				}
			} else {
				newAsksSurvivedTime.average()
			}
		)
		
		return true
	}
	
	
	private fun opsAround(opsMsgs: ArrayDeque<Message<BookOperations>>, percent: Double): Int {
		return opsMsgs.flatMap { it.content }
			.filter { it.bookEntry.price > bidAskMid.last * (1 - percent) && it.bookEntry.price < bidAskMid.last * (1 + percent) }
			.count()
	}
	
	fun resetAll() {
		this.forEach { it.reset() }
		hasValidIndicators = false
	}
	
	private fun areValid() = all { it.isValid() }
	
	
	
	// 0.05 range at 10k price ---> [9500, 10500]
	private fun BookOperation.inRange(range: Double): Boolean =
		this.bookEntry.price > bidAskMid.last * (1 - range) && this.bookEntry.price < bidAskMid.last * (1 + range)
	
	private val BookOperation.netAmount: Double
		get() = if (this is BookOperation.Delete) {
			-1 * this.bookEntry.amount
		} else this.bookEntry.amount
		
		
}