@file:JvmName("Buyer")
package tothrosch.instrument.database.reader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import tothrosch.engine.ConnectionTest.exchange
import tothrosch.engine.Global
import tothrosch.engine.Keep
import tothrosch.engine.message.ConnectionState
import tothrosch.engine.message.Message
import tothrosch.instrument.*
import tothrosch.instrument.book.*
import tothrosch.instrument.book.operations.BookOperation
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.instrument.database.DatabaseException
import tothrosch.instrument.database.reader.DataReader.feedbackChannel
import tothrosch.settings.Settings
import tothrosch.util.Directory
import tothrosch.util.time.TimeScope
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class InstrumentReader(
	val dir: Directory,
	broadcastChannel: BroadcastChannel<Long>,
	private val dataReaderFeedbackChannel: Channel<Unit>,
	override val coroutineContext: CoroutineContext
) : CoroutineScope, TimeScope {
	
	override val time = DataReader.time
	private val timeSubscription: ReceiveChannel<Long> = broadcastChannel.openSubscription()
	private val fileDates: List<Long> = dir.listFiles()!!.filterNot { it.name.contains("data.") }
		.map { it.name.substringBefore(".data").toLong() }.sorted()
	
	/*	val startTime: Long = max(
			fileDates.firstOrNull() ?: throw RuntimeException("$exchange does not have any files to read"),
			fileDates.getNext(Settings.Backtest.startLong)
		)*/
	val endTime: Long = fileDates.last() + Settings.dataIntervalLength
	
	// throw some exceptions which doesn't do anything because we call this constructor with 'try'
	init {
		if (fileDates.firstOrNull() == null) throw RuntimeException("$exchange does not have any files to read")
		if (endTime < Settings.Backtest.startLong) {
			cancelChannelSubscription()
			throw ExceptionInInitializerError("could not create InstrumentReader on dir $dir because all files are too early")
		}
	}
	
	// private val historicCandles = runBlocking { getHistoricCandles() }
	private var currentStartTime: Long = fileDates.getNext(Settings.Backtest.startLong)
	private var currentEndTime: Long = currentStartTime + Settings.dataIntervalLength
	private var currentFile: File = File(dir.path + File.separator + currentStartTime.toString() + ".data")
	private var currentLinesIterator = currentFile.readLines().iterator()
	private var currentIndex = fileDates.indexOf(currentStartTime)
	private var currentLineElements = getNextLineElements()
	// val jacksonObjectMapper = ObjectMapper().registerModule(KotlinModule(nullIsSameAsDefault = true))
	private val mapper = ObjectMapper().registerModule(KotlinModule(nullIsSameAsDefault = true)) // because of proguard
	private var hasMoreMessages = true
	
	@Volatile
	private var lastMsgReadTime: Long = 0L
	
	val instrument: Instrument = getInstrumentFromFirstLine()
	// .apply { runBlocking { getHistoricCandles().forEach { historicCandles.add(it) } } }
	
	var count = Random.nextLong(1, 2000)
	
	val job = launch(start = CoroutineStart.LAZY) {
		try {
			for (until in timeSubscription) {
				
				// println("reader at ${instrument.symbol} got $time")
				processMessages(until)
				// println("reader at ${instrument.symbol} processed messages at ${until}")
				sendBatchEnd()
				
				if (hasMoreMessages == false) {
					Global.instruments.remove(instrument)
					cancelChannelSubscription()
					break
				}
				// println("reader at ${instrument.symbol} processed $time")
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
			println("${instrument.exchange.name} ${instrument.pair} instrument reader broke down")
		}
		println("InstrumentReader for  ${instrument.symbol} closed")
	}
	
	private fun getInstrumentFromFirstLine(): Instrument {
		val currentTime = currentStartTime + currentLineElements[0].toLong()
		val instrument: Instrument = mapper.readValue(currentLineElements.drop(1).joinToString(), Instrument::class.java)
		instrument.time.update(currentTime)
		currentLineElements = getNextLineElements()
		return instrument
	}
	
	private suspend fun processMessages(until: Long) {
		
		
		while (lastMsgReadTime <= until) {
			//while(lastMsgReadTime < nextMs) {
			//if (count % 200000L == 0L) {
			//	println("${DataReader.instrumentReaders.map { it.lastMsgReadTime }.joinToString(", ")}}")
			//}
			count++
			
			when (currentLineElements[1].first()) {
				'O'      -> {
					instrument.bookOpsHandler.handleMessage(getBookOperationMessage(currentLineElements) as Message<BookOperations>)
					// instrument.channel.send(getBookOperationMessage(currentLineElements))
				}
				'T'      -> {
					instrument.tradeMessageHandler.handleMessage(createTradeMessage(currentLineElements) as Message<Trades>)
					// instrument.channel.send(createTradeMessage(currentLineElements))
				}
				'U'      -> {
					instrument.channel.send(createUpdateMessage(currentLineElements))
				}
				'L', '{' -> {
					instrument.channel.send(createStateMessage(currentLineElements))
				}
				'S'      -> {
					val message: Message<Book>? = try {
						createBookSnapshotMessage(currentLineElements)
					} catch (ex: Exception) {
						null
					}
					message?.let {
						instrument.bookHandler.handleMessage(it)
						//instrument.channel.send(it as Message<Any>)
					}
				}
				else     -> {
					println(currentLineElements.joinToString(","))
					throw DatabaseException("could not parse line ${currentLineElements.joinToString { "," }} at ${instrument.symbol} on ${instrument.exchange.name} on file ${currentFile.path}")
				}
			}
			// println("processing ${currentLineElements.joinToString()}")
			currentLineElements = getNextLineElements()
			lastMsgReadTime = currentStartTime + currentLineElements[0].toLong()
		}
	}
	
	fun getNextLineElements(): List<String> {
		if (currentLinesIterator.hasNext() == false) {
			val successLoading = loadNextFile()
			if (successLoading == false) {
				// set time super high
				return listOf("${Long.MAX_VALUE / 2}")
			}
		}
		return currentLinesIterator.next().split(",")
		
		/*var line: String? = currentReader.readLine()
		if (line == null) {
			if (loadNextFile()) {
				line = currentReader.readLine()
			} else {
				// set time super high
				return listOf("${Long.MAX_VALUE / 2}")
			}
		}
		if (line == null) {
			return listOf("${Long.MAX_VALUE / 2}")
		}
		return line.split(",")*/
	}
	
	// returns success
	var loadingMs = 0L
	fun loadNextFile(): Boolean {
		currentIndex++
		return if (fileDates.size <= currentIndex) {
			println("InstrumentReader for ${instrument.symbol} about to close")
			hasMoreMessages = false
			instrument.isActive = false
			false
		} else {
			// println("loaded next file")
			val start = System.currentTimeMillis()
			currentStartTime = fileDates[currentIndex]
			currentEndTime = currentStartTime + Settings.dataIntervalLength
			currentFile = File(dir.path + File.separator + currentStartTime.toString() + ".data")
			currentLinesIterator = currentFile.readLines().iterator()
			val end = System.currentTimeMillis()
			loadingMs += end - start
			true
		}
	}
	
	
	fun createStateMessage(elements: List<String>): Message<Any> {
		val state: ConnectionState = when (elements[2]) {
			"N"  -> ConnectionState.DISCONNECTED
			else -> ConnectionState.CONNECTED
		}
		return Message(state, timestamp = currentStartTime + elements[0].toLong())
	}
	
	fun createTradeMessage(elements: List<String>): Message<Any> {
		val time: Long = currentStartTime + elements[0].toLong()
		val trades: Trades = parseTrades(elements[2], time)
		return Message(trades, timestamp = time)
	}
	
	fun parseTrades(unparsed: String, time: Long): Trades = Trades(unparsed.split("/")
		                                                               .map { parseTrade(it.split(":"), time) })
	
	fun parseTrade(elements: List<String>, time: Long) = Trade(price = elements[1].toDouble(),
	                                                           amount = elements[2].toDouble(),
	                                                           initiatingSide = stringToSide(elements[0]),
	                                                           time = time)
	
	fun getBookOperationMessage(elements: List<String>): Message<Any> {
		val time: Long = currentStartTime + elements[0].toLong()
		val ops: BookOperations = getOps(elements[2], time)
		if (ops.filter { it is BookOperation.Change && it.bookEntry.amount == 0.0 }.isNotEmpty()) {
			println()
		}
		return Message(ops, timestamp = time)
	}
	
	fun getOps(unparsedOps: String, time: Long) =
		BookOperations(unparsedOps.split("/").mapNotNull { getOp(it.split(":"), time) })
	
	fun getOp(opStringList: List<String>, time: Long, isLiquidation: Boolean = false): BookOperation? {
		when (opStringList.size) {
			2       -> return BookOperation.Delete(side = stringToSide(opStringList[0]),
			                                       bookEntry = BookEntry(price = 0.0,
			                                                             amount = 0.0,
			                                                             id = opStringList[1],
			                                                             time = time,
			                                                             isLiquidation = isLiquidation))
			4       -> {
				val price = opStringList[2].toDouble()
				val amount = opStringList[3].toDouble()
				if (price != 0.0 && amount != 0.0) {
					return BookOperation.Insert(side = stringToSide(opStringList[0]),
					                            bookEntry = BookEntry(price = price,
					                                                  amount = amount,
					                                                  id = opStringList[1],
					                                                  time = time,
					                                                  isLiquidation = isLiquidation))
					
				} else return null
			}
			3       -> {
				val amount = opStringList[2].toDouble()
				if (amount != 0.0) {
					return BookOperation.Change(side = stringToSide(opStringList[0]),
					                            deltaEntry = BookEntry(price = 0.0,
					                                                   amount = amount,
					                                                   id = opStringList[1],
					                                                   time = time,
					                                                   isLiquidation = isLiquidation))
				} else return null
			}
			5, 6, 7 -> return getOp(opStringList = opStringList.dropLast(3),
			                        time = time,
			                        isLiquidation = true)
			else    -> {
				throw DatabaseException("could not parse bookoperation: ${opStringList.joinToString(":")}")
			}
		}
	}
	
	
	private fun createUpdateMessage(elements: List<String>): Message<Any> {
		val time: Long = currentStartTime + elements[0].toLong()
		val updateJsonString = elements.drop(2).joinToString(",")
		val update: Update = mapper.readTree(updateJsonString) as ObjectNode
		return Message(update, timestamp = time)
	}
	
	private fun createBookSnapshotMessage(elements: List<String>): Message<Book> {
		// elements: time,S,book
		// book: id:price:amount/id:price:amount/..../id:price:amount$$$$$id:price:amount/..../id:price:amount
		val time: Long = currentStartTime + elements[0].toLong()
		val (bidsString, asksString) = elements[2].split("$$$$$")
		val bidsMutable: BidsMutable = toMutableSide(bidsString, time, BidsComparator) as BidsMutable
		val asksMutable: AsksMutable = toMutableSide(asksString, time, AsksComparator) as AsksMutable
		return Message(Book(bidsMutable = bidsMutable, asksMutable = asksMutable, time = time), timestamp = time)
	}
	
	private fun stringToSide(sideString: String): Side = when (sideString) {
		"B"  -> Side.BUY
		"S"  -> Side.SELL
		else -> throw DatabaseException("could not get side from string $sideString")
	}
	
	// each string: id:price:amount/id:price:amount/..../id:price:amount
	private fun toMutableSide(unparsed: String, time: Long, comparator: BookEntryComparator): BookSideMutable {
		try {
			val entries: List<BookEntry> = unparsed.split("/").map { it.split(":") }
				.map { BookEntry(it[1].toDouble(), it[2].toDouble(), it[0], time) }
				.filterNot { it.price == 0.0 || it.amount == 0.0 }
			val entryMap = HashMap<String, BookEntry>(entries.associate { it.id to it })
			val entryTree = TreeSet(entries.toSortedSet(comparator))
			// TODO
			if (entries.any { it.amount == 0.0 }) {
				println()
			}
			return when (comparator) {
				is BidsComparator -> BidsMutable(entryMap, entryTree)
				is AsksComparator -> AsksMutable(entryMap, entryTree)
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
			println()
			return BidsMutable()
		}
	}
	
	/*private suspend fun getHistoricCandles(): List<HistoricCandle> {
		we don't really need them anyway'
		val totalLength = Settings.historicCandleAmount * Settings.historicCandleAmount
		val historicDates = fileDates.filter { it + totalLength > startTime && it < startTime }
		val candleCreator = HistoricCandleCreator(this, historicDates)
		return candleCreator.getCandles()
	}*/
	
	
	private suspend fun sendBatchEnd() {
		// println("reader at ${instrument.symbol} sending batch end to instrument.commandhandler")
		instrument.channel.send(Message(feedbackChannel))
	}
	
	
	fun List<Long>.getNext(until: Long): Long {
		for (i in this.indices) {
			if (this[i] >= until) {
				return this[i]
			}
		}
		return this.last()
	}
	
	fun cancelChannelSubscription() = timeSubscription.cancel()
}