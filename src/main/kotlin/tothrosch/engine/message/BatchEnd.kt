package tothrosch.engine.message

import kotlinx.coroutines.channels.Channel


class BatchEnd(val feedbackChannel: Channel<Unit>)



