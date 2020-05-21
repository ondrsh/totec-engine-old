package tothrosch.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tothrosch.exchange.Exchange
import tothrosch.instrument.Mode
import tothrosch.instrument.database.reader.DataReader


/**
 * Created by ndrsh on 27.05.17.
 */

var mode: Mode = Mode.START

object App {
	@JvmStatic
	fun main(args: Array<String>) {
		runBlocking {
			println("starting app")
			processArguments(args)
			initialize()

			when (mode.isLive) {
				true -> {
					Global.exchanges.forEach {
						if (it.instruments.isNotEmpty()) {
							it.liveFeed.reconnectingChannel.send(it.instruments.first())
						}
					}
					// TODO remove this, should still stay alive because of coroutinescope
					while (true) {
						delay(10_000)
					}
				}
				false -> {
					// TODO remove this, should still stay alive because of coroutinescope
					while (DataReader.broadCastChannel.isClosedForSend == false) {
						delay(1000)
					}
					println("finally over")
					return@runBlocking
				}
			}
			
		}
	}
	
	private suspend fun processArguments(args: Array<String>) {
		if (args.size != 1) throw RuntimeException("wrong number of arguments")
		val argument = args[0].toUpperCase()
		if (argument == "TEST") {
			ConnectionTest.latency()
		} else {
			mode = Mode.valueOf(args[0].toUpperCase())
			if (mode == Mode.START) throw IllegalArgumentException("mode cannot be start")
		}
	}
	
	fun initialize() {
		if (mode == Mode.START) throw RuntimeException("mode cannot be starting")
		Global.exchanges.forEach { it.initialize() }
		loadInstruments()
		Global.exchanges.startInstruments()
		
		if (mode.isLive) Global.exchanges.startLiveFeeds()
		else DataReader.job.start()
	}
	
	fun loadInstruments() {
		Global.exchanges.forEach { ex ->
			ex.instruments.forEach { inst ->
				Global.startPairs.add(inst.pair)
				Global.startCurrencies.add(inst.pair.base)
				Global.startCurrencies.add(inst.pair.quote)
			}
		}
		Global.instruments.addAll(Global.exchanges.flatMap { it.instruments }.toSet())
	}
	
}


fun Set<Exchange>.startLiveFeeds() = this.forEach { it.liveFeed.job.start() }
fun Set<Exchange>.startInstruments() = this.forEach { ex -> ex.instruments.forEach { it.job.start() } }
