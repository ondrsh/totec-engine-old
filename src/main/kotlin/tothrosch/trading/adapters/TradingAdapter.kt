package tothrosch.trading.adapters

import org.apache.http.client.methods.HttpRequestBase
import tothrosch.engine.message.OrderRequest
import tothrosch.engine.message.Request
import tothrosch.engine.mode
import tothrosch.instrument.Mode
import tothrosch.trading.adapters.backtest.Backtest


interface TradingAdapter {
	
	fun sendRequest(request: OrderRequest)
	
	companion object {
		fun create() = when (mode) {
			Mode.TRADE                        -> BitmexTradingAdapter()
			Mode.BACKTEST, Mode.BACKTEST_LIVE -> Backtest
			else                              -> throw RuntimeException("creating trading adapter although we are not in either TRADE nor BACKTEST mode")
		}
	}
}
