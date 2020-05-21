package tothrosch.instrument.handlers

import kotlinx.coroutines.channels.Channel
import tothrosch.engine.message.Message

/*abstract class MessageHandler<T>(channelSize: Int, protected val channel: Channel<Message<T>> = Channel(channelSize)) :
	Channel<Message<T>> by channel {
	abstract suspend fun handleMessage(msg: Message<T>): Message<T>?
}*/

abstract class MessageHandler<T, R>(channelSize: Int, protected val channel: Channel<Message<T>> = Channel(channelSize)) :
	Channel<Message<T>> by channel {
	
	abstract fun handleMessage(msg: Message<T>): Message<R>?
}
