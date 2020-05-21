package tothrosch.engine.message

import tothrosch.networking.ConnectionType
import tothrosch.util.time.log.EventLog
import tothrosch.util.time.log.LogList

open class Message<T>(var content: T,
                      var error: Error? = null,
                      val type: ConnectionType = ConnectionType.FEED,
                      var timestamp: Long = System.currentTimeMillis(),
                      override val log: LogList? = null) : EventLog {
    
    fun kill() {
        TODO()
    }
}

