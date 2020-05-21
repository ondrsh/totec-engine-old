/*
package tothrosch.trading.networking.livefeed

import kotlinx.coroutines.experimental.channels.SendChannel
import tothrosch.trading.instrument.Instrument
import tothrosch.trading.instrument.messages.Message


*/
/**
 * Created by ndrsh on 26.05.17.
 *//*




abstract class WebsocketClientAbstractClass(accountChannel: SendChannel<Message>,
                                            globalInstruments: List<Instrument>):   {

    abstract val address: String
    var session: javax.websocket.Session? = null
    var container: javax.websocket.WebSocketContainer = javax.websocket.ContainerProvider.getWebSocketContainer()


    fun sendMessage(message: String) = session?.asyncRemote?.sendText(message)


    override fun rawConnect() {
        container.connectToServer(this, java.net.URI.create(address))
    }

    override fun onOpen(session: javax.websocket.Session?, config: javax.websocket.EndpointConfig?)   {
        this.session = session
        val messageHandler = javax.websocket.MessageHandler.Whole<String> {
            kotlinx.coroutines.experimental.runBlocking { liveFeedChannel.send(it) }
        }

        session?.addMessageHandler(messageHandler)
        log("onOpen triggered. Connection: ${if (session?.isOpen ?: false) "open" else "closed"}")
    }

    override fun onClose(session: javax.websocket.Session?, closeReason: javax.websocket.CloseReason?) {
        log("onClose triggered. Reason: ${closeReason?.toString() ?: "unknown reason"}")
    }

    override fun onError(session: javax.websocket.Session?, thr: Throwable?) {
        // log(thr?.message ?: "unknown error")
        log("onError triggered. Error: ${thr?.message ?: "unknown error"}")
        session?.close()
        Thread.sleep(2000)
        log("trying to reconnect now")
        rawConnect()
    }

}
*/
