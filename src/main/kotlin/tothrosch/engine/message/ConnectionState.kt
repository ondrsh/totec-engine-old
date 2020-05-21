package tothrosch.engine.message

class ConnectionUpdate(val connectionState: ConnectionState)

enum class ConnectionState { CONNECTED, DISCONNECTED;
	
	override fun toString() = if (this == CONNECTED) "Y" else "N"
}