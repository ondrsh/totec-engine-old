package tothrosch.networking.livefeed

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.settings.Settings
import tothrosch.util.*
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import kotlin.coroutines.CoroutineContext


/**
 * Created by ndrsh on 13.06.17.
 */

abstract class LiveFeed(protected val exchange: Exchange) : CoroutineScope by exchange, TimeScope  {
	
	override val time: Time = Time.Live
	
	private var initialConnection = false
	@Volatile var lastBookMessage: Long = System.currentTimeMillis()

	val reconnectingChannel = Channel<Instrument?>(10)
	val msgChannel = Channel<String>(10_000)
	
	val job = launch(start = CoroutineStart.LAZY) {
		
		// coroutine for reconnectingChannel
		launch {
			var lastReconnectionTry = 0L
			while (isActive) {
				try {
					for (inst in reconnectingChannel) {
						if (lastReconnectionTry.ago < 100_000) continue
						if (initialConnection) {
							disconnect()
							if (inst != null) {
								log("${exchange.name} disconnected, trying to re-connect because of ${inst.pair} - last bookmessage is ${(1.0 * lastBookMessage.ago) / 1000} seconds ago")
							} else {
								log("${exchange.name} disconnected trying to re-connect")
							}
							exchange.instruments.forEach { it.bookInit?.reset() }
						}
						log("${exchange.name} trying to connect")
						val startTime = System.currentTimeMillis()
						connect()
						log("${exchange.name} successfully connected, took ${(1.0 * now - startTime) / 1000.0} seconds")
						println()
						initialConnection = true
						lastReconnectionTry = now
					}
				} catch (ex: Exception) {
					log("${exchange.name} reconnectingJob error, continuing at next reconnectMessage -->")
					ex.printStackTrace()
					lastReconnectionTry = now
				}
			}
		}
		
		// coroutine for message processing
		launch {
			while (isActive) {
				try {
					for (msg in msgChannel) {
						processMessage(msg)
					}
				} catch (ex: Exception) {
					log("${exchange.name} livefeedJob broke down --> ")
					ex.printStackTrace()
				}
			}
		}
	}


	protected abstract suspend fun processMessage(msg: String)
	protected abstract fun rawConnect()
	abstract fun disconnect()
	protected abstract suspend fun subscribe()


	open fun initialize() {}


	fun connect(): Boolean {
		try {
			rawConnect()
			// subscribe()
		} catch (ex: Exception) {
			println("${exchange.name} had error while connection, retrying soon I think")
			println(ex.message)
			ex.printStackTrace()
			return false
		}
		return true
	}


	/*companion object {
		fun getInstance(exchange: Exchange): LiveFeed = when (exchange) {
			is Exchange.BithumbExchange  -> BithumbLivefeed(exchange)
			is Exchange.BitfinexExchange -> BitfinexWebsocket(exchange)
			is Exchange.BitmexExchange   -> BitmexWebsocket(exchange)
			is Exchange.BitstampExchange -> BitstampLivefeed(exchange)
			else                         -> throw RuntimeException("implement this")
			*//*Exchange.Impl.GDAX      -> GdaxWebsocket(exchange)
			Exchange.Impl.GEMINI    -> GeminiLivefeed(exchange)
			Exchange.Impl.OKEX      -> OkexFuturesWebsocket(exchange)
			Exchange.Impl.POLONIEX  -> PoloniexWebsocket(exchange)*//*
		}
	}*/

}
