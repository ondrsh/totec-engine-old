package tothrosch.networking

/*
class RestMode(val instrument: Instrument)  {
    var active: Boolean = false
    var lastActivated: Long = 0L
    var lastDeactivated: Long = 0L
    var lastBookRequest: Long = 0L

    fun activate()  {
        if(!active) lastActivated = now
        active = true
        log(instrument.time.now, "rest mode activated for exchange ${instrument.exchange.impl}")
    }

    fun deactivate()  {
        if(active)  lastDeactivated = now
        active = false
        log(instrument.time.now, "rest mode deactivated for exchange ${instrument.exchange.impl}")
    }

    suspend fun handleBookRequest()  {
        if(lastBookRequest.ago > Settings.restBookInterval)
            requestBook()
    }

    suspend fun requestBook()  {
        instrument.exchange.restClient.job.send(Request.Rest.Book(instrument.bookMessageHandler, instrument))
        lastBookRequest = now
    }
}*/
