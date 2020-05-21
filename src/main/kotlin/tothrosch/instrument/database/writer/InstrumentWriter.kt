package tothrosch.instrument.database.writer

import tothrosch.engine.message.ConnectionState
import tothrosch.engine.message.Message
import tothrosch.instrument.Instrument
import tothrosch.instrument.Trades
import tothrosch.instrument.Update
import tothrosch.instrument.book.Book
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.settings.Settings
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.math.max

class InstrumentWriter(val instrument: Instrument) {

	var currentTimeArea: Long = instrument.time.now / Settings.dataIntervalLength * Settings.dataIntervalLength
	lateinit var rootDirectory: String
	lateinit var file: File
	private var wait: Boolean = false
	private lateinit var writer: BufferedWriter
	private val msgBuffer: ArrayList<String> = arrayListOf()
	var initialized = false


	fun initialize() {
		rootDirectory = Settings.dataPath + File.separator + instrument.exchange.name + File.separator + instrument
		file = File(rootDirectory + File.separator + currentTimeArea + ".data")
	}

	// accepts:
	// 1) BookOperationMessage
	// 2) TradeMessage
	// 3) Update
	// 4) TimeMessage --> if TimeMessage, make new file and then return
	fun receiveMsg(msg: Message<*>) {
		try {
			if (initialized == false) {
				updateTimeArea(msg.timestamp)
				if (wait == false) {
					// this is always up to date when mode is live (and writing is live)
					if (instrument.book.isPopulated() == false) return
					setUpNewFile()
					writeFileStart()
					initialized = true
					return
				}
				initialized = true
			}
			if (outOfDate(msg)) {
				updateTimeArea(msg.timestamp)
				nextFile()
			}
			if (wait || msg.content is Boolean) return
			if (msgBuffer.size > 500) writeBuffer()
			bufferMsg(msg)
		} catch (ex: Exception) {
			ex.printStackTrace()
			println("write error at ${instrument.pair} at ${instrument.exchange.name}")
		}
	}

	private fun outOfDate(msg: Message<*>): Boolean = msg.timestamp > currentTimeArea + Settings.dataIntervalLength

	private fun nextFile() {
		if (wait == false) {
			writeBuffer()
			writer.close()
		}
		wait = false
		setUpNewFile()
		writeFileStart()
	}

	private fun setUpNewFile() {
		file = File(rootDirectory + File.separator + currentTimeArea + ".data")
		if (file.exists() == false) file.parentFile.mkdirs()
		if (file.exists()) file.delete()
		file.createNewFile()
		writer = BufferedWriter(FileWriter(file.absoluteFile))
	}

	private fun writeBuffer() {
		for (line: String in msgBuffer) {
			writer.newLine()
			writer.write(line)
		}
		msgBuffer.clear()
		writer.flush()

	}

	private fun bufferMsg(msg: Message<*>) {
		val timeDelta: Long = getTimeDelta()
		val lineToAdd: String =
			when (msg.content) {
				is Trades          -> "$timeDelta,T,${(msg.content as Trades).joinToString("/")}"
				is BookOperations  -> "$timeDelta,O,${(msg.content as BookOperations).joinToString("/")}"
				is Update          -> "$timeDelta,U,${msg.content}"
				is ConnectionState -> "$timeDelta,L,${msg.content}"
				else               -> throw RuntimeException("cannot determine type of message $msg")
			}
		msgBuffer.add(lineToAdd)
	}

	private fun writeFileStart() {
		writeInstrument()
		writer.newLine()
		writeBook()
		writer.newLine()
		writeConnectionInfo()
		writer.flush()
	}

	private fun writeInstrument() {
		val timeDelta = getTimeDelta()
		val lineToWrite = "$timeDelta,${instrument.toJson()}"
		writer.write(lineToWrite)
	}

	private fun writeBook() {
		val timeDelta: Long = getTimeDelta()
		val lineToWrite = "$timeDelta,${instrument.book.toDb()}"
		writer.write(lineToWrite)
	}
	
	private fun writeConnectionInfo() {
		val timeDelta: Long = getTimeDelta()
		val lineToWrite = if (instrument.isConnected) "$timeDelta,L,Y" else "$timeDelta,L,N"
		writer.write(lineToWrite)
	}


	private fun updateTimeArea(time: Long) {
		currentTimeArea = time / Settings.dataIntervalLength * Settings.dataIntervalLength
	}

	private fun getTimeDelta(): Long = max(instrument.time.now, currentTimeArea) - currentTimeArea


}