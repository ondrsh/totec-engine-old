/*
package tothrosch.trading.networking.livefeed

import com.fasterxml.jackson.databind.JsonNode
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.util.*

*/
/**
 * Created by ndrsh on 20.06.17.
 *//*


// purpose of this class is to get the orderbook of an instrument via REST, while livefeed gets the diffs
// diff messages are stored into msgList while REST order is done
// if sending == false, make livefeed call sendBookRequest
// keep checking if result != null at each message for this instrument
// if result (the orderbook as json) is there, make livefeed 1) process the orderbook and then
// 2) process each message in msgList that has sequence > bookSequence (that's why it's a treemap>
// 3) ...????
// 4) PROFIT


class Initialization(val addressParts: List<String>,
                          val sym: String,
                         */
/* val bookProcessor: BookProcessor,
                          val liveFeed: LiveFeed,*//*

                          var initialized: Boolean = false,
                          var msgList: TreeMap<Long, JsonNode> = TreeMap(),
                          var result: Result<String, FuelError>? = null,
                          var sending: Boolean = false,
                          var failed: Boolean = false,
                          var bookSequence: Long = Long.MAX_VALUE)   {


    suspend fun removeEarlyMessages()   {
        msgList.keys.removeIf{it <= bookSequence}
    }


    suspend fun sendBookRequest(sym: String)   {
        val address: String = addressParts.get(0) + sym + addressParts.get(1)
        //GdaxApiService.create().processRestBook(BookSnapshott, seqquence, timee... guten morgen tothrosch <3)
        address.httpGet().responseString { _request, response, result ->
            //do something with response
            when (result) {
                is Result.Failure -> {
                    this.failed = true
                    log(result.get())
                }
                is Result.Success -> {
                    if(response.httpStatusCode/100 == 4)  {
                        this.failed = true
                    }
                    else    {
                        this.result = result
                    }
                }
            }
        }
    }

}*/
