package tothrosch.instrument.database.reader

import kotlinx.coroutines.channels.Channel
import tothrosch.engine.message.Message

class LastMessage<T>(val lastMessage: Message<T>, val lastChannel: Channel<Message<T>>) {
	
	suspend fun send() {
		lastChannel.send(lastMessage)
	}
}

