package tothrosch.instrument.handlers

import kotlinx.coroutines.channels.Channel
import tothrosch.engine.message.Message
import tothrosch.engine.message.Request

// unlike MessageHandler, this class does not return any messages

abstract class FinalMessageHandler<T>(channelSize: Int,
                                      protected open val channel: Channel<Message<T>> = Channel(channelSize)) : Channel<Message<T>> by channel {
	
	abstract suspend fun handleMessage(msg: Message<T>)
}
