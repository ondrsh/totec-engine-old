package tothrosch.trading.adapters.backtest

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import tothrosch.engine.Global
import tothrosch.engine.message.Error
import tothrosch.engine.message.Message
import tothrosch.engine.message.OrderEvent.*
import tothrosch.engine.message.OrderRequest
import tothrosch.instrument.Side
import tothrosch.instrument.Trade
import tothrosch.instrument.Trades
import tothrosch.instrument.book.BookEntry
import tothrosch.instrument.book.BookSideMutable
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.settings.Settings
import tothrosch.trading.Hub
import tothrosch.trading.adapters.TradingAdapter
import tothrosch.trading.orders.BitmexOrders
import tothrosch.trading.orders.BitmexOrder
import tothrosch.util.quickSend
import tothrosch.util.round
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import tothrosch.util.time.log.Event
import kotlin.coroutines.CoroutineContext

// TODO care, we are sometimes having specific assumptions about int and double (bitmexorders int and entries double), but ok for bitmex only
// this acts like an exchange engine, receiving and returning messages from the strategy
object Backtest : TradingAdapter, TimeScope, CoroutineScope {
	
	override val coroutineContext: CoroutineContext = Settings.appContext
	override val time: Time = Global.tradingInstrument.time
	private val active: HashMap<String, BitmexOrder> = HashMap()
	val orderChannel = Channel<OrderRequest>(5)
	private val inst = Global.tradingInstrument
	
	override fun sendRequest(request: OrderRequest) {
		orderChannel.quickSend(request)
	}
	
	fun receiveRequest(req: OrderRequest): OrderRequest? {
		return handleRequest(req)
	}
	
	private fun handleRequest(request: OrderRequest): OrderRequest {
		if (request.isValid() == false) sendBackError(request)
		else {
			when (request.orderEvent) {
				AMEND                      -> request.updateOrders(request.map { amend(it) })
				// always makers
				POST                       -> request.updateOrders(request.map { post(it, take = false) })
				CANCEL, CANCELALL          -> request.updateOrders(request.map { cancel(it) })
				// these are takers... maybe cancel other orders before closing position?
				POST_SINGLE, CLOSEPOSITION -> request.updateOrders(request.map { post(it, take = true) })
			}
			request.addEvent(Event.BACKTESTING_ORDERREQUEST_HANDLED)
			request.returnChannel.quickSend(request)
		}
		return request
	}
	
	fun handleAllCanceledFromBook() {
		val msg = Message(BitmexOrders(active.map { it.value.getCancelSuccess() }))
		Hub.orderHandler.quickSend(msg)
		active.clear()
	}
	
	// TODO we are not handling trade sorting order, which doesn't matter for bitmex, but should be kept in mind
	fun handlePublicTrades(msg: Message<Trades>) {
		val tradesBySide = msg.content.groupBy { it.initiatingSide }
		for (sideToTrades in tradesBySide) {
			inst.book[sideToTrades.key.opposite].handlePublicTrades(sideToTrades.value)
		}
	}
	
	private fun BookSideMutable.handlePublicTrades(trades: List<Trade>) {
		val tradeIter = trades.iterator()
		var trade = if (tradeIter.hasNext()) tradeIter.next() else return
		var tradeAmount = trade.amount.toInt()
		var cumQty = 0
		
		fun loadNextTrade() = if (tradeIter.hasNext()) {
			trade = tradeIter.next()
			tradeAmount = trade.amount.toInt()
			cumQty = 0
			true
		} else false
		
		val myOrdersAffected = mutableListOf<BitmexOrder>()
		val opsToDoAfter = mutableListOf<BookOperation>()
		for (entry in set) {
			// if trade is fully filled, load new one (or break if iterator dead)
			if (cumQty == tradeAmount) {
				if (loadNextTrade() == false) break
			} else if (cumQty > tradeAmount) throw BacktestException("cumQty larger than trade.amount... maybe rounding mistake? $cumQty > ${trade.amount}")
			// if this trade does not hit the entry, load new trades until they either hit or iterator is dead
			while (trade.hitsEntry(entry) == false) {
				if (loadNextTrade() == false) break
			}
			// remaining trading amount is larger than entry.amount, so we eat entry completely
			if (tradeAmount - cumQty >= entry.amount) {
				if (entry.isMine) {
					myOrdersAffected.addAndProcessFill(entry)
					opsToDoAfter.add(BookOperation.Delete(side = side, bookEntry = entry))
				}
				cumQty += entry.amount.toInt()
			}
			// amount left in the trade will only reduce entry
			else {
				if (entry.isMine) {
					myOrdersAffected.addAndProcessPartialFill(entry, amountToFill = tradeAmount - cumQty)
					opsToDoAfter.add(BookOperation.Change(side = side,
					                                      deltaEntry = entry.copy(amount = -1.0 * (tradeAmount - cumQty),
					                                                              time = trade.time,
					                                                              survivedTime = trade.time - entry.time)))
				}
				cumQty = tradeAmount
			}
			
		}
		processAllBookOps(opsToDoAfter)
		if (myOrdersAffected.isNotEmpty()) {
			Hub.orderHandler.quickSend(Message(content = BitmexOrders(myOrdersAffected),
			                                   timestamp = now))
		}
	}
	

	
	
	private fun sendBackError(orderRequest: OrderRequest) {
		orderRequest.error = Error(400, "Bad Request")
		orderRequest.returnChannel.quickSend(orderRequest)
	}
	
	// actually posts the order in the book and returns the correct posted, working BitmexOrder
	private fun post(order: BitmexOrder, take: Boolean): BitmexOrder {
		return if (take) {
			val bookSide = inst.book[order.side!!.opposite]
			meltMyTrade(order, bookSide)
		} else {
			val bookSide = inst.book[order.side!!]
			if (bookSide.addOrder(createBookEntryToInsert(post = order))) {
				bookSide.updateNecessary = true
				order.getPostSuccess()
			} else throw BacktestException("could not add post order, order = $this, book = $bookSide")
		}
	}
	
	// actually amends the order in the book and returns the correct amended, working BitmexOrder
	private fun amend(newAmend: BitmexOrder): BitmexOrder {
		val orig = active[newAmend.origClOrdID]!! // must exist because request is valid
		val bookSide = inst.book[orig.side!!]
		return if (bookSide.addOrder(createBookEntryToInsert(oldAmend = orig,
		                                                     newAmend = newAmend))
			&& bookSide.map.remove(orig.clOrdID!!) != null) {
			bookSide.updateNecessary = true
			newAmend.getAmendSuccess(orig)
		} else throw BacktestException("could not add amended order, old = $orig, new = $this, book = $bookSide")
	}
	
	// actually cancels the order in the book and returns the correct canceled BitmexOrder
	private fun cancel(order: BitmexOrder): BitmexOrder {
		val bookSide = inst.book[order.side!!]
		return if (bookSide.map.remove(order.clOrdID!!) != null) {
			bookSide.updateNecessary = true
			order.getCancelSuccess()
		}
		else throw BacktestException("could not cancel order, order = $this, book = $bookSide")
	}
	
	// melts my taking BitmexOrder through the orderbook and returns the (partially) filled order
	private fun meltMyTrade(order: BitmexOrder, bookSide: BookSideMutable): BitmexOrder {
		var cumQty = order.cumQty ?: 0
		val opsToDoAfter = mutableListOf<BookOperation>()
		val myOtherOrdersAffected = mutableListOf<BitmexOrder>()
		for (entry in bookSide.set) {
			// finished, trade fully filled
			if (cumQty == order.orderQty!!) break
			if (cumQty > order.orderQty) throw BacktestException("cumQty larger than order.orderQty.. maybe rounding mistake? $cumQty > ${order.orderQty}")
			if (order.hitsEntry(entry)) {
				val entryAmount = entry.amount.toInt()
				// amount left in the trade will kill entry
				if (order.orderQty - cumQty >= entryAmount) {
					if (entry.isMine) myOtherOrdersAffected.addAndProcessFill(entry)
					opsToDoAfter.add(BookOperation.Delete(side = bookSide.side, bookEntry = entry))
					cumQty += entry.amount.toInt()
				}
				// amount left in the trade will only reduce entry
				else {
					if (entry.isMine) myOtherOrdersAffected.addAndProcessPartialFill(entry = entry,
					                                                                 amountToFill = order.orderQty - cumQty)
					opsToDoAfter.add(BookOperation.Change(side = bookSide.side,
					                                      deltaEntry = entry.copy(amount = -1.0 * (order.orderQty - cumQty),
					                                                              time = order.timestamp,
					                                                              survivedTime = order.timestamp - entry.time)))
					cumQty = order.orderQty
				}
			}
			// if order does not hit, break
			else break
		}
		bookSide.processAllBookOps(opsToDoAfter)
		if (myOtherOrdersAffected.isNotEmpty()) {
			Hub.orderHandler.quickSend(Message(content = BitmexOrders(myOtherOrdersAffected),
			                                   timestamp = now))
		}
		return when (cumQty) {
			(order.cumQty ?: 0) -> throw BacktestException("order $order should have hit $bookSide but cumQty stayed the same")
			order.orderQty      -> order.getFillSuccess()
			else                -> order.getPartialFillSuccess(cumQty)
		}
	}
	
	// used when iterating through bookSide because of 1) public trades or 2) my takers
	private fun MutableList<BitmexOrder>.addAndProcessFill(entry: BookEntry) {
		val myUpdatedOrder = active[entry.id]!!.getFillSuccess()
		this.add(myUpdatedOrder)
		active.remove(entry.id)
	}
	
	// same as above
	private fun MutableList<BitmexOrder>.addAndProcessPartialFill(entry: BookEntry, amountToFill: Int) {
		val myUpdatedOrder = active[entry.id]!!.getPartialFillSuccess(amountToFill)
		this.add(myUpdatedOrder)
		active[entry.id] = myUpdatedOrder
	}
	
	private fun BitmexOrder.hitsEntry(entry: BookEntry) = if (side!! == Side.SELL) price!! <= entry.price else price!! >= entry.price
	
	// creates the book entry to insert when we want to post
	private fun createBookEntryToInsert(post: BitmexOrder) = BookEntry(price = post.price!!,
	                                                                   amount = post.orderQty!!.toDouble(),
	                                                                   id = post.clOrdID!!,
	                                                                   time = now,
	                                                                   isMine = true)
	
	// creates the book entry to insert when we want to amend
	private fun createBookEntryToInsert(oldAmend: BitmexOrder, newAmend: BitmexOrder) =
		BookEntry(price = newAmend.price ?: oldAmend.price!!,
		          amount = if (newAmend.orderQty != null) {
			          (newAmend.orderQty.toDouble() - (oldAmend.cumQty?.toDouble() ?: 0.0)).round(11)
		          } else (oldAmend.orderQty!!.toDouble() - (oldAmend.cumQty?.toDouble() ?: 0.0)).round(11),
		          id = newAmend.clOrdID!!,
		          time = now,
		          isMine = true)
	
	// updates the BitmexOrder as the Bitmex engine would do when the post succeeds
	private fun BitmexOrder.getPostSuccess() = update(ordStatus = BitmexOrder.Status.NEW,
	                                                  workingIndicator = true,
	                                                  timestamp = now)
	
	// updates the BitmexOrder as the Bitmex engine would do when the amend succeeds
	private fun BitmexOrder.getAmendSuccess(orig: BitmexOrder) = orig.updateWith(this).update(origClOrdID = null,
	                                                                                          ordStatus = BitmexOrder.Status.NEW,
	                                                                                          workingIndicator = true,
	                                                                                          timestamp = now)
	
	// update the BitmexOrder as the Bitmex engine would do when the cancel succeeds
	private fun BitmexOrder.getCancelSuccess() = update(ordStatus = BitmexOrder.Status.CANCELED,
	                                                    workingIndicator = false,
	                                                    timestamp = now)
	
	// updates the BitmexOrder as the Bitmex engine would do when it was partially filled
	private fun BitmexOrder.getPartialFillSuccess(cumQty: Int) = update(cumQty = cumQty,
	                                                                    ordStatus = BitmexOrder.Status.PARTIALLY_FILLED,
	                                                                    workingIndicator = true,
	                                                                    timestamp = now)
	
	private fun BitmexOrder.getFillSuccess() = update(cumQty = orderQty,
	                                                  ordStatus = BitmexOrder.Status.FILLED,
	                                                  workingIndicator = false,
	                                                  timestamp = now)
	
	
	// checks if the request is valid, especially if maker posts and amends can be inserted into the book without triggering
	private fun OrderRequest.isValid(): Boolean {
		when (orderEvent) {
			POST                       -> return content.all { it.isValidMaker(it.side!!) }
			AMEND                      -> return content.all {
				val orig = active[it.origClOrdID!!] ?: return false
				if (it.price == null) return true
				return it.isValidMaker(orig.side!!)
			}
			// these are always takers
			POST_SINGLE, CLOSEPOSITION -> return content.all { it.isValidTaker() }
			CANCEL, CANCELALL          -> return content.all { active.containsKey(it.clOrdID!!) }
		}
	}
	
	// checks if maker can be inserted 1) without triggering immediately and 2) without falling out of the set
	private fun BitmexOrder.isValidMaker(side: Side): Boolean {
		return when (side) {
			Side.BUY  -> {
				price!! < inst.book[Side.SELL].set.first().price && price > inst.book[Side.BUY].set.last().price + 10.0
			}
			Side.SELL -> {
				price!! > inst.book[Side.BUY].set.first().price && price < inst.book[Side.SELL].set.last().price - 10.0
			}
		}
	}
	
	// checks if the taker would hit the book. if it doesn't we return false (because takers
	// are sent with IMMEDIATEORCANCEL)
	private fun BitmexOrder.isValidTaker(): Boolean {
		// take care, we have to use opposite sides
		return when (side!!) {
			Side.BUY  -> price!! >= inst.book[Side.SELL].set.first().price
			Side.SELL -> price!! <= inst.book[Side.BUY].set.first().price
		}
	}
	
	private fun Trade.hitsEntry(bookEntry: BookEntry) = when (initiatingSide) {
		Side.BUY  -> price >= bookEntry.price
		Side.SELL -> price <= bookEntry.price
	}
	

	
	//TODO bussi und viel spaß beim coden!
	
	class BacktestException(msg: String) : Exception(msg)
}