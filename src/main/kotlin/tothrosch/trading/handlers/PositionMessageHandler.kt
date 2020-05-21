package tothrosch.trading.handlers

import tothrosch.engine.message.Message
import tothrosch.instrument.handlers.FinalMessageHandler
import tothrosch.trading.Hub
import tothrosch.trading.position.bitmex.BitmexPosition
import tothrosch.util.log
import tothrosch.util.round
import tothrosch.util.time.TimeScope

class PositionMessageHandler(private val strategy: Hub) : FinalMessageHandler<BitmexPosition>(100), TimeScope by strategy {
	
	
	// in backtest mode, position gets only created/updates by MyTradesBacktestHandler
	// in trade mode, position gets only created/updated by OrderHandler
	// positionHandler generally does NOT update position, only other stuff like
	// avgEntryPrice, liquidationPrice etc...
	
	// so we don't use this to actually update the position (except at initialization!!)
	// we use adjustPosition() in TradingHub instead to update the position
	// because we can never know if these position updates arrive just before the filled orders arrive
	
	override suspend fun handleMessage(msg: Message<BitmexPosition>) {
		// only update currentQty at initialization, so when we connect we update the position
		strategy.position = strategy.position.copy(openOrderBuyQty = msg.content.openOrderBuyQty,
												   openOrderSellQty = msg.content.openOrderSellQty,
												   avgEntryPrice = msg.content.avgEntryPrice,
												   liquidationPrice = msg.content.liquidationPrice,
												   timestamp = msg.content.timestamp,
		                                           currentQty = if (strategy.position.timestamp == 0L) {
			                                           msg.content.currentQty
		                                           } else strategy.position.currentQty)
		
		log("position is ${strategy.position.currentQty}," +
					"avgEntryPrice is ${strategy.position.avgEntryPrice?.round(2)}," +
					"liquididationPrice is ${strategy.position.liquidationPrice?.round(1)}")
	
		// println("new position: ${trading.position.currentQty}")
	}
}