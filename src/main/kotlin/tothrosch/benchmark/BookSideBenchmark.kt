package tothrosch.benchmark

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import tothrosch.engine.mode
import tothrosch.instrument.Side
import tothrosch.instrument.book.BidsMutable
import tothrosch.instrument.book.BookEntry
import tothrosch.instrument.book.operations.BookOperation
import java.util.concurrent.TimeUnit
import kotlin.random.Random

val rand = Random(2345623456)

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
open class BookSideBenchmark {
	
	val bookSide = BidsMutable()
	
	@Setup
	fun setVariable() {
		mode = tothrosch.instrument.Mode.READ
	}
	
	// @benchmark
	fun bookSpeed() {
		bookSide.processAllBookOps(createOp())
	}
	
	// @benchmark
	fun createOp(): List<BookOperation> {
		val price = rand.nextInt(9000, 10000).toDouble()
		val entry = BookEntry(price = price,
		                      amount = rand.nextDouble(1.0, 100_000.0),
		                      id = price.toString(),
		                      time = 0L)
		val op = BookOperation.Insert(Side.BUY, entry)
		return listOf(op)
	}
	
	val context = Dispatchers.Unconfined
	
	val channel = Channel<List<BookOperation>>(1)
	val job = GlobalScope.async(context) {
		for (msg in channel) {
			bookSide.processAllBookOps(msg)
		}
	}
	
	@Benchmark
	fun createOpViaChannel() = runBlocking(context) {
		channel.send(createOp())
	}
	
}

fun main() {
	val options = OptionsBuilder()
		.include(BookSideBenchmark::class.java.simpleName)
		.output("benchmark_sequence.log")
		.build()
	Runner(options).run()
}
