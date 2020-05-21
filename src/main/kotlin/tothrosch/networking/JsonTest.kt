package tothrosch.networking

/*
package tothrosch.trading.websocket

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableHashMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.experimental.runBlocking
import tothrosch.trading.settings.Settings.mainPool
import tothrosch.trading.benchmark.simpleMeasureTest
import tothrosch.trading.instrument.book.AsksComparator
import tothrosch.trading.instrument.book.BidsComparator
import tothrosch.trading.instrument.book.Book
import tothrosch.trading.instrument.book.BookEntry
import java.util.*
import kotlin.collections.HashMap
import kotlin.system.measureNanoTime

*/
/**
 * Created by ndrsh on 25.05.17.
 *//*



object JsonTest {

    @JvmStatic
    fun main(args: Array<String>) = runBlocking(mainPool) {


        val testJson = getWebsocketBookAsJson()
        val jacksonMapper = jacksonObjectMapper()
        val jacksonJsonNode: JsonNode? = jacksonMapper.readTree(testJson).get("data")


        val testBook: Book = Book()


        val bidsSet: ImmutableSet<BookEntry> = jacksonJsonNode?.filter { it.get("side").asText() == "Buy" }?.map { BookEntry(it.get("price").asDouble(), it.get("size").asDouble(), it.get("id").asText()) }?.toSortedSet(BidsComparator)?.toImmutableSet() ?: throw Exception()
        val bidsMap: ImmutableMap<String, BookEntry> = bidsSet?.map { it.id to it  }.toMap().toImmutableHashMap()
        val asksSet: ImmutableSet<BookEntry> = jacksonJsonNode?.filter { it.get("side").asText() == "Sell" }?.map { BookEntry(it.get("price").asDouble(), it.get("size").asDouble(), it.get("id").asText()) }?.toSortedSet(AsksComparator)?.toImmutableSet() ?: throw Exception()
        val asksMap: ImmutableMap<String, BookEntry> = asksSet?.map { it.id to it  }.toMap().toImmutableHashMap()

        testBook.bidsMutable.replaceAll(bidsSet, bidsMap)
        testBook.asksMutable.replaceAll(asksSet, asksMap)
        val copyBook = testBook.clone()


        // Jackson
        simpleMeasureTest(ITERATIONS = 1000, WARM_COUNT = 20, TEST_COUNT = 15)   {

            val copyBook = testBook.clone()


            val bidsSet = TreeSet<BookEntry>(BidsComparator)
            val bidsMap = HashMap<String, BookEntry>()
            val asksSet = TreeSet<BookEntry>(AsksComparator)
            val asksMap = HashMap<String, BookEntry>()

            for(node: JsonNode in jacksonJsonNode!!)    {
                val side = node.get("side").asText()
                val entry = BookEntry(price = node.get("price").asDouble(), amount = node.get("size").asDouble(), id = node.get("id").asText())

                when(side)  {
                    "Buy"   -> {
                        bidsSet.add(entry)
                        bidsMap.put(entry.id, entry)
                    }
                    "Sell"  -> {
                        asksSet.add(entry)
                        asksMap.put(entry.id, entry)
                    }
                }
            }

            bidsSet.toImmutableSet()
            bidsMap.toImmutableHashMap()
            asksSet.toImmutableSet()
            asksMap.toImmutableHashMap()

            */
/*testBook.bidsMutable.replaceAll(bidsSet.toImmutableSet(), bidsMap.toImmutableHashMap())
            testBook.asksMutable.replaceAll(asksSet.toImmutableSet(), asksMap.toImmutableHashMap())
*//*

        }





        var bid: BookEntry? = null

        println((measureNanoTime {
            for(x in 1..1_000_000) {
                bid = testBook.bidsMutable.idToEntry.get("8799777320")
            }
        }) / 1_000_000.0)


        println("bid has price ${bid?.price}")
    }

    fun wasteTime() {
        val dumbList: DoubleArray = DoubleArray(1_000_000, {it -> Math.sqrt(it * 1.0)})
    }

    fun testName() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



}



val useless: Boolean = false*/
