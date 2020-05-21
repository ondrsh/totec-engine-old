package tothrosch.networking.livefeed

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.client.WebSocketClient
import tothrosch.engine.message.ConnectionState
import tothrosch.engine.message.Message
import tothrosch.exchange.Exchange
import tothrosch.util.logLive
import java.net.ConnectException
import java.util.concurrent.CompletionStage


/**
 * Created by ndrsh on 26.05.17.
 */

abstract class WebsocketClient(exchange: Exchange) : LiveFeed(exchange)  {
	
	abstract val address: String
	private lateinit var client: WebSocketClient
	private lateinit var session: Session
	@Volatile var lastFullChannelMsg: Long = System.currentTimeMillis()
	
	
	override fun rawConnect() {
		// try {
		if (::client.isInitialized == false) {
			client = WebSocketClient()
			client.httpClient.connectTimeout = 5000
		}
		client.start()
		@Suppress("BlockingMethodInNonBlockingContext", "UNCHECKED_CAST")
		client.connect(this, java.net.URI.create(address)) as CompletionStage<Session>
		
		/*} catch (ex: Exception) {
			ex.printStackTrace()
			log(now, "${exchange.name} couldn't rawConnect, took ${(1.0 * now - startTime) / 1000.0} seconds, trying to reconnect in 50 seconds")
			try {
				session?.close()
			} catch (ex: Exception) {
			}
			session = null
			delay(50_000)
		}*/
	}
	
	
	override fun disconnect() {
		try {
			session.close()
		} catch (ex: Exception) {
			logLive("${exchange.name} failed to close session - probably was closed already")
		}
	}
	
	fun sendMessage(message: String) {
		session.remote.sendStringByFuture(message)
	}
	
	@OnWebSocketConnect
	fun onConnect(session: Session) {
		this.session = session
		launch { subscribe() }
		runBlocking { exchange.instruments.forEach { it.channel.send(Message(ConnectionState.CONNECTED)) } }
	}
	
	
	@OnWebSocketClose
	fun onClose(session: Session, closeCode: Int, closeReason: String?) {
		println("websocket on ${exchange.name} closed, reason is $closeReason, code is $closeCode")
		this.session = session
		disconnect()
		runBlocking { exchange.instruments.forEach { it.channel.send(Message(ConnectionState.DISCONNECTED)) } }
	}
	
	@OnWebSocketMessage
	fun onMessage(message: String) {
		val hasCapacity = msgChannel.offer(message)
		if (hasCapacity == false ) {
			logLive("${exchange.name} channel is full, dropping (!!!) messages")
			lastFullChannelMsg = System.currentTimeMillis()
			logLive("SHUTDOWN")
			job.cancel()
		}
	}
	
	
	@OnWebSocketError
	fun onError(session: Session, thr: Throwable) {
		logLive("${exchange.name} websocket onError triggered. Error: ${thr?.message ?: "unknown error"}")
		thr.printStackTrace()
		if (thr is ConnectException) {
			// Attempt to reconnect
			reconnectingChannel.offer(exchange.instruments.first())
		} else {
			// Ignore upgrade exception
		}
		session.close()
	}
}