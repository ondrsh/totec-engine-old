package tothrosch.trading.adapters

import org.apache.http.client.methods.HttpRequestBase
import tothrosch.engine.message.OrderEvent.*
import tothrosch.engine.message.OrderRequest
import tothrosch.exchange.Exchange.BitmexExchange.restClient
import tothrosch.networking.rest.adapter.bitmex.BitmexRestClient

class BitmexTradingAdapter : TradingAdapter {
	
	override fun sendRequest(request: OrderRequest) {
		BitmexRestClient.executeRequest(http = request.getHttpRequest(),
		                                request = request)
		
		
	}
	
	private fun OrderRequest.getHttpRequest() = when (this.orderEvent) {
		POST                       -> restClient.getPostBulkHttpRequest(this)
		POST_SINGLE, CLOSEPOSITION -> restClient.getPostHttpRequest(this)
		AMEND                      -> restClient.getAmendBulkHttpRequest(this)
		CANCEL                     -> restClient.getCancelHttpRequest(this)
		CANCELALL                  -> restClient.getCancelAllHttpRequest(this)
	}
}