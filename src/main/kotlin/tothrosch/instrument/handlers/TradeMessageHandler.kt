package tothrosch.instrument.handlers

import tothrosch.engine.message.Message
import tothrosch.engine.mode
import tothrosch.instrument.*
import tothrosch.networking.ConnectionType
import tothrosch.trading.adapters.backtest.Backtest
import tothrosch.util.time.TimeScope

open class TradeMessageHandler(val instrument: Instrument) : MessageHandler<Trades, Trades>(1000) {
	
	var lastTrade: Triple<Trade, ConnectionType, Long> = Triple(Trade(price = 0.0,
	                                                                  amount = 0.0,
	                                                                  initiatingSide = Side.BUY), ConnectionType.REST, 0L)
	
	// this is important so we don't get extremely old trades on the first request
	// instead, the first request is only used to determine the correct lastTrade object
	var initialized = false
	
	override fun handleMessage(msg: Message<Trades>): Message<Trades>? {
		instrument.time.update(msg.timestamp)
		filterTrades(msg)?.let {
			if (mode.needsSamplers) {
				instrument.samplers.addTradesMsg(msg)
			}
			initialized = true
			return it
		}
		// msg.content?.let { debug(it) }
		return null
	}
	
	fun debug(trades: Trades) {
		println("Trades on Exchange ${instrument.exchange.name} on pair ${instrument.pair} ${trades.joinToString(   )}    WORTH ${trades.map { it.amount * it.price }.sum()}")
	}
	
	private fun filterTrades(msg: Message<Trades>): Message<Trades>? {
		if (msg.type == ConnectionType.FEED) {
			lastTrade = Triple(msg.content.last(), ConnectionType.FEED, msg.content.last().time)
			return msg
		} else {
			// REST
			// only bithumb uses REST trades so far
			// trades from old to new, so last trade in msg = last trade
			var tradesToTake = 0
			tradeCounter@ for (i in (msg.content.size - 1) downTo 0) {
				val trade = msg.content[i]
				if (trade.time > lastTrade.third) {
					tradesToTake++
				} else if (trade.time < lastTrade.third) {
					break@tradeCounter
				} else {
					if (trade.price != lastTrade.first.price || trade.amount != lastTrade.first.amount) {
						tradesToTake++
					} else {
						break@tradeCounter
					}
				}
			}
			
			if (tradesToTake > 0) {
				lastTrade = Triple(msg.content.last(), ConnectionType.REST, msg.content.last().time)
				if (initialized == false) {
					return null
				}
				msg.content = Trades(trades = msg.content.takeLast(tradesToTake), sequence = msg.content.sequence)
				return msg
			}
		}
		return null
		
	}
	
	companion object {
		fun create(instrument: Instrument) = if (instrument.isTrading  && (mode.isBacktesting)) BacktestTradeMessageHandler(instrument) else TradeMessageHandler(instrument)
	}
	
}

// This exists because at Backtesting, we have to look if these trades would have hit our orders.
// Make sure that the channel we send to understands what the BookOperations mean (see function below).
class BacktestTradeMessageHandler(instrument: Instrument): TradeMessageHandler(instrument), TimeScope by instrument {
	
	override fun handleMessage(msg: Message<Trades>): Message<Trades>? {
		instrument.time.update(msg.timestamp)
		Backtest.handlePublicTrades(msg)
		return super.handleMessage(msg)
	}
}