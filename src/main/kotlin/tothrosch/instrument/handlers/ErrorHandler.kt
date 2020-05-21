/*
package tothrosch.trading.instrument.handlers

import kotlinx.coroutines.experimental.channels.Channel
import tothrosch.trading.engine.message.Message
import tothrosch.trading.engine.message.Request
import tothrosch.trading.engine.message.RequestType
import tothrosch.trading.instrument.Instrument
import tothrosch.trading.util.log
import tothrosch.trading.util.now

class ErrorHandler(val instrument: Instrument, private val channel: Channel<Message<*>> = Channel(100)): Channel<Message<*>> by channel {


    suspend fun handleMessage(msg: Message<*>): Message<*>? {
        if (msg.error!!.originalRequest is Request.Rest && msg.error.originalRequest?.type == RequestType.Book) {
            log(now, "${instrument.exchange.impl} error at handling REST Request --> couldn't get Book from RestClient")

            instrument.bookInit?.let {
                if (!it.initialized && it.sending) {
                    it.sending = false
                }
            }
        }
        return null
    }

}*/
