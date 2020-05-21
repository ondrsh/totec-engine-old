package tothrosch.instrument.handlers

import tothrosch.engine.message.ConnectionState
import tothrosch.engine.message.ConnectionState.*
import tothrosch.engine.message.Message
import tothrosch.engine.mode
import tothrosch.instrument.Instrument
import tothrosch.util.quickSend
import tothrosch.util.time.LiveTimerJob
import tothrosch.util.time.TimeScope

class ConnectionHandler(val instrument: Instrument) : MessageHandler<ConnectionState, ConnectionState>(10), TimeScope by instrument {
	
	private lateinit var liveTimerJob: LiveTimerJob
	
	
	override fun handleMessage(msg: Message<ConnectionState>): Message<ConnectionState> {
		instrument.time.update(msg.timestamp)
		when (msg.content) {
			CONNECTED    -> {
				instrument.isConnected = true
			}
			DISCONNECTED -> {
				instrument.isConnected = false
				if (mode.isLive) safeReconnect()
			}
		}
		return msg
	}
	
	private fun safeReconnect() {
		instrument.exchange.liveFeed.reconnectingChannel.quickSend(instrument)
	}
	
	fun createAndStartLiveTimerJob() {
		liveTimerJob = LiveTimerJob(interval = 1000,
		                            startingDelay = 30_000,
		                            coroutineContext = instrument.coroutineContext) {
			if (instrument.seemsLive() == false) {
				sendDisconnectMessage()
			}
		}.apply { job.start() }
	}
	
	fun sendDisconnectMessage() = quickSend(Message(DISCONNECTED))
}