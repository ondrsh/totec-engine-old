package tothrosch.networking.livefeed.adapter

import org.eclipse.jetty.websocket.api.annotations.WebSocket
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.networking.livefeed.WebsocketClient


@WebSocket(maxTextMessageSize = 65536 * 1000)
class OkexFuturesWebsocket(exchange: Exchange) : WebsocketClient(exchange) {
	
	override val address: String = "wss://real.okex.com:10440/websocket/okexapi"
	val channelToInst: HashMap<String, Instrument> = hashMapOf()
	// type 0 = book, type 1 = trade
	val channelToType: HashMap<String, Int> = hashMapOf()
	
	
	suspend override fun processMessage(msg: String) {
		println(msg)
	}
	
	suspend override fun subscribe() {
		sendMessage("""{'event':'addChannel','channel':'ok_sub_futureusd_btc_depth_this_week'}""")
		sendMessage("""{'event':'addChannel','channel':'ok_sub_futureusd_btc_trade_this_week'}""")
	}
}