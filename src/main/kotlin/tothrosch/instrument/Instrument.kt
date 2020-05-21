package tothrosch.instrument


import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tothrosch.engine.Keep
import tothrosch.engine.message.BatchEnd
import tothrosch.engine.message.ConnectionState
import tothrosch.engine.message.Message
import tothrosch.engine.mode
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.Currency
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.Mode.*
import tothrosch.instrument.book.Book
import tothrosch.instrument.book.BookInit
import tothrosch.instrument.book.operations.BookOperations
import tothrosch.instrument.candle.samplers.Sampler
import tothrosch.instrument.database.writer.InstrumentWriter
import tothrosch.instrument.handlers.*
import tothrosch.instrument.handlers.bookhandlers.BookHandler
import tothrosch.instrument.handlers.bookhandlers.BookOperationsHandler
import tothrosch.json.TimestampDeserializer
import tothrosch.json.TimestampSerializer
import tothrosch.json.bitmex.BitmexCurrencyDeserializer
import tothrosch.settings.Settings
import tothrosch.util.log
import tothrosch.util.time.Time
import tothrosch.util.time.TimeScope
import tothrosch.util.toDateCode
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Created by ndrsh on 28.05.17.
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY,
	property = "type"
)
/*@JsonSubTypes(
	JsonSubTypes.Type(value = Instrument.Spot::class),
	JsonSubTypes.Type(value = Instrument.Bitmex.Swap::class),
	JsonSubTypes.Type(value = Instrument.Bitmex.Side.Down::class),
	JsonSubTypes.Type(value = Instrument.Bitmex.Side.Up::class),
	JsonSubTypes.Type(value = Instrument.Bitmex.Future::class)
)*/
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Keep
sealed class Instrument(
	val pair: CurrencyPair,
	// we are ignoring exchanges on JSON because we are creating them from directories when reading
	val exchange: Exchange,
	@JsonProperty("symbol") val symbol: String
) : TimeScope, CoroutineScope by exchange {
	
	
	@JsonIgnore override val time: Time = if (mode.isLive) Time.Live else Time.Read()
	@delegate:JsonIgnore val isTrading: Boolean by lazy { Settings.isTrading(this) }
	
	
	@JsonIgnore val channel = Channel<Message<Any>>(if (mode.isLive) 100 else 1)
	
	// can either be TradeMessageHandler or BacktestTradeMessageHandler
	// TODO make sure this is not accessing something before complete instrument is created
	@delegate:JsonIgnore val tradeMessageHandler by lazy { TradeMessageHandler.create(this) }
	
	// TODO make sure this is not accessing something before complete instrument is created
	@delegate:JsonIgnore val bookHandler by lazy { BookHandler(this) }
	
	// TODO make sure this is not accessing something before complete instrument is created
	@delegate:JsonIgnore val bookOpsHandler by lazy { BookOperationsHandler(this) }
	
	// TODO make sure this is not accessing something before complete instrument is created
	@delegate:JsonIgnore val instrumentUpdateHandler by lazy { InstrumentUpdateHandler(this) }
	
	//@Suppress("LeakingThis")
	@JsonIgnore val connectionHandler = ConnectionHandler(this)
	
	//@Suppress("LeakingThis")
	// @JsonIgnore val commandHandler = CommandHandler(this)
	@JsonIgnore val batchEndChannel = Channel<BatchEnd>(1)
	
	abstract val dirString: String
	// connectionHandler has coroutine (TimerJob) - start it
	
	
	@JsonIgnore val samplers = Sampler(this)
	
	// @Suppress("LeakingThis")
	// @JsonIgnore val historicCandles = HistoricCandles(this)
	@JsonIgnore val tradeMsgQueue: ArrayDeque<Message<Trades>> = ArrayDeque()
	@JsonIgnore val bookOpsMsgQueue: ArrayDeque<Message<BookOperations>> = ArrayDeque()
	
	@JsonIgnore val book: Book = Book()
	@JsonIgnore var consistency = Consistency()
	@JsonIgnore val bookInit: BookInit? = if (exchange.restInitNecessary && mode.isLive) BookInit() else null
	val initialized: Boolean
		@JsonIgnore
		get() = book.bidsMutable.set.size > 0 && book.asksMutable.set.size > 0 && book.time.ago < 60_000
	@JsonIgnore var isConnected: Boolean = false
	
	// val instrumentReader: InstrumentReader? = if(mode == READ) InstrumentReader(this) else null
	@JsonIgnore lateinit var instrumentWriter: InstrumentWriter
	
	@Volatile
	@JsonIgnore var isActive = true
	
	@JsonIgnore var guiActive = false
	
	// @JsonIgnoreProperties("jframe")
	// @JsonIgnore var guiFrame: JFrame? = null
	@JsonIgnore val mapper: ObjectMapper = jacksonObjectMapper().disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
	@JsonIgnore var msgCount = 0
	
	open fun processInstrumentUpdate(node: JsonNode) {}
	
	open fun toJson(): ObjectNode = mapper.convertValue(this, ObjectNode::class.java)
	
	
	val job = launch(start = CoroutineStart.LAZY) {
		if (mode.needsSamplers) samplers.initialize()
		try {
			if (mode.isBacktesting) {
				// set instrumentWriter and LiveTimerJob
				if (mode == WRITE) instrumentWriter = InstrumentWriter(this@Instrument)
				connectionHandler.createAndStartLiveTimerJob()
				while (isActive) {
					for (msg in channel) {
						@Suppress("UNCHECKED_CAST")
						val finalMessage: Message<*>? = when (msg.content) {
							is BookOperations  -> bookOpsHandler.handleMessage(msg as Message<BookOperations>)
							is Book            -> bookHandler.handleMessage(msg as Message<Book>)
							is Trades          -> tradeMessageHandler.handleMessage(msg as Message<Trades>)
							is Update          -> instrumentUpdateHandler.handleMessage(msg as Message<Update>)
							is ConnectionState -> connectionHandler.handleMessage(msg as Message<ConnectionState>)
							else               -> {
								null
							}
						}
						
						/*	val finalMessage: Message<*>? = select {
						// commandHandler.onReceive { commandHandler.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						if (mode.isBacktesting) Backtest.orderChannel.onReceive { Backtest.receiveRequest(it) }
						this@Instrument.channel.onReceive { it.feedbackChannel.quickSend(Unit)
							null
						}
					}*/
						
						if (mode == WRITE) passToWriter(finalMessage)
					}
				}
			} else {
				// not live
				while (isActive) {
					for (msg in channel) {
						
						@Suppress("UNCHECKED_CAST")
						val finalMessage: Message<*>? = when (msg.content) {
							is BookOperations  -> bookOpsHandler.handleMessage(msg as Message<BookOperations>)
							is Book            -> bookHandler.handleMessage(msg as Message<Book>)
							is Trades          -> tradeMessageHandler.handleMessage(msg as Message<Trades>)
							is Update          -> instrumentUpdateHandler.handleMessage(msg as Message<Update>)
							is ConnectionState -> connectionHandler.handleMessage(msg as Message<ConnectionState>)
							is Channel<*>   -> {
								(msg.content as Channel<Unit>).send(Unit)
								null
							}
							else               -> {
								null
							}
						}
						
				/*			val finalMessage: Message<*>? = select {
						// commandHandler.onReceive { commandHandler.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						this@Instrument.channel.onReceive { this@Instrument.channel.handleMessage(it) }
						if (mode.isBacktesting) Backtest.orderChannel.onReceive { Backtest.receiveRequest(it) }
						this@Instrument.channel.onReceive { it.feedbackChannel.quickSend(Unit)
							null
						}
					}*/
					}
				}
			}
		} catch (ex: Exception) {
			log("${exchange.name} instrument fundamental error, breaking instrument coroutine -->")
			ex.printStackTrace()
			println()
		}
		
	}
	
	
	fun processTrades(timestamp: Long, trades: Trades) {
		val mut = mutableListOf<Trade>()
		mut.clear()
		
	}
	
	fun passToWriter(msg: Message<*>?) {
		if (msg == null || msg.content is Book || initialized == false) return
		instrumentWriter.receiveMsg(msg)
	}
	
	private fun handleGui() {
	/*	if (guiActive == false) {
			@JsonIgnore
			guiFrame = CreateGUI.createGUI(this)
			guiActive = true
		}
		
		val bidsModel: TableModel? = (guiFrame?.contentPane as BookJPanel).bids.model
		val asksModel: TableModel? = (guiFrame?.contentPane as BookJPanel).asks.model
		val bids = book.bidsMutable.set.take(50)
		val asks = book.asksMutable.set.take(50)
		val size = min(bids.size, asks.size)
		
		if (size > 0) {
			for (j in 0..min(size - 1, 9)) {
				bidsModel?.setValueAt(bids[j].price, j, 0)
				bidsModel?.setValueAt(bids[j].amount, j, 1)
				asksModel?.setValueAt(asks[j].price, j, 0)
				asksModel?.setValueAt(asks[j].amount, j, 1)
			}
			
			if (size < 10) {
				for (j in size..9) {
					bidsModel?.setValueAt(0.0, j, 0)
					bidsModel?.setValueAt(0.0, j, 1)
					asksModel?.setValueAt(0.0, j, 0)
					asksModel?.setValueAt(0.0, j, 1)
				}
			}
		} else {
			for (j in 0..9) {
				bidsModel?.setValueAt(0.0, j, 0)
				bidsModel?.setValueAt(0.0, j, 1)
				asksModel?.setValueAt(0.0, j, 0)
				asksModel?.setValueAt(0.0, j, 1)
			}
		}*/
	}
	
	// TODO check this, here we just book.isConcistent() instead of the seemsConsistent() function
	fun handleLiveConsistency() {
		if (book.isConsistent() == false) {
			if (time.now - consistency.lastInconsistentMsg > 50_000) {
				consistency.lastInconsistentMsg = time.now
				log(
					"${exchange.name} - inconsistent state at $pair" + if (mode.isLive == false) "" else ", trying to reconnect"
				)
				log(
					"Bid: ${book.bidsMutable.set.first().price}, Ask: ${book.asksMutable.set.first().price}"
				)
				log(
					"# Bids Set: ${book.bidsMutable.set.size}, # Bids Map: ${book.bidsMutable.map.size}"
				)
				log(
					"# Asks Set: ${book.asksMutable.set.size}, # Asks Map: ${book.asksMutable.map.size}"
				)
				println()
				connectionHandler.sendDisconnectMessage()
			}
		}
	}
	
	@JvmName("tradeNow")
	fun seemsConsistent(): Boolean {
		val bookConsistent = book.isConsistent()
		
		if (bookConsistent == false) {
			consistency.count++
			if (consistency.count > 20 && consistency.lastConsistent.ago > (if (mode.isLive) 15_000 else 15_000)) return false
			return true
		} else {
			consistency.lastConsistent = time.now
			consistency.count = 0
			return true
		}
	}
	
	fun seemsLive() = exchange.liveFeed.lastBookMessage.ago < Settings.liveMaxTimeOut
	
	fun consistentForFeatures() = book.isConsistent() && seemsLive()
	
	
	fun clearQueues() {
		bookOpsMsgQueue.clear()
		tradeMsgQueue.clear()
	}
	
	
	override fun toString(): String = when (this) {
		is Spot          -> symbol.replace("/", "") //_SPOT"
		is Bitmex.Future -> "${symbol}_FUTURE_${this.settle.toDateCode()}_${this.listing.toDateCode()}"
		is Bitmex.Swap   -> symbol //_SWP"
		is Bitmex.Side   -> "${symbol}_${this.settle.toDateCode()}_${this.listing.toDateCode()}"
	}
	
	
	class Consistency {
		var count: Int = 0
		var lastInconsistentMsg: Long = 0L
		var lastConsistent: Long = 0L
	}
	
	@JsonTypeName("spot")
	@Keep
	class Spot(
		pair: CurrencyPair,
		@JacksonInject exchange: Exchange,
		symbol: String
	) : Instrument(pair, exchange, symbol) {
		
		override val dirString = "SPOT"
	}
	
	@Keep
	sealed class Bitmex(
		symbol: String,
		//@JsonDeserialize(using = BitmexCurrencyDeserializer::class) val positionCurrency: Currency,
		@JsonProperty("underlying") @JsonDeserialize(using = BitmexCurrencyDeserializer::class) val underlyingCurrency: Currency,
		@JsonDeserialize(using = BitmexCurrencyDeserializer::class) val quoteCurrency: Currency,
		pair: CurrencyPair = CurrencyPair(underlyingCurrency, quoteCurrency),
		@JacksonInject exchange: Exchange,
		
		var state: String,
		@JsonDeserialize(using = TimestampDeserializer::class)
		@JsonSerialize(using = TimestampSerializer::class)
		open val listing: Instant?,
		@JsonDeserialize(using = TimestampDeserializer::class)
		@JsonSerialize(using = TimestampSerializer::class)
		open val settle: Instant?,
		var isQuanto: Boolean,
		var isInverse: Boolean,
		var capped: Boolean,
		var tickSize: Double,
		var lotSize: Double,
		
		var makerFee: Double,
		var takerFee: Double,
		var settlementFee: Double,
		var indicativeSettlePrice: Double,
		var markPrice: Double,
		var lowPrice: Double,
		var highPrice: Double,
		var turnover24h: Long,
		var openInterest: Long,
		var openValue: Long,
		var vwap: Double,
		var limitDownPrice: Double? = null,
		var limitUpPrice: Double? = null,
		var volume24h: Long,
		var volume: Long
	) : Instrument(pair, exchange, symbol) {
		
		abstract val periodLength: Duration
		@JsonIgnore var lastMarkPriceUpdate: Long = 0L
		
		override fun processInstrumentUpdate(node: JsonNode) {
			for ((key, value) in node.fields()) {
				if (value.asText() == "null") {
					continue
				}
				when (key) {
					"state"                 -> this@Bitmex.state = value.asText()
					"markPrice"             -> {
						this@Bitmex.markPrice = value.asDouble()
						this@Bitmex.lastMarkPriceUpdate = now
					}
					"openInterest"          -> this@Bitmex.openInterest = value.asLong()
					// in dollars
					"volume24h"             -> this@Bitmex.volume24h = value.asLong()
					"vwap"                  -> this@Bitmex.vwap = value.asDouble()
					"highPrice"             -> this@Bitmex.highPrice = value.asDouble()
					"lowPrice"              -> this@Bitmex.lowPrice = value.asDouble()
					"lotSize"               -> this@Bitmex.lotSize = value.asDouble()
					"tickSize"              -> this@Bitmex.tickSize = value.asDouble()
					"isQuanto"              -> this@Bitmex.isQuanto = value.asBoolean()
					"isInverse"             -> this@Bitmex.isInverse = value.asBoolean()
					"makerFee"              -> this@Bitmex.makerFee = value.asDouble()
					"takerFee"              -> this@Bitmex.takerFee = value.asDouble()
					"settlementFee"         -> this@Bitmex.settlementFee = value.asDouble()
					"capped"                -> this@Bitmex.capped = value.asBoolean()
					"openValue"             -> this@Bitmex.openValue = value.asLong()
					// in satoshis
					"turnover24h"           -> this@Bitmex.turnover24h = value.asLong()
					"indicativeSettlePrice" -> this@Bitmex.limitUpPrice = value.asDouble()
					"limitDownPrice"        -> this@Bitmex.limitDownPrice = value.asDouble()
					"limitUpPrice"          -> this@Bitmex.limitUpPrice = value.asDouble()
					else                    -> processIndidivualField(key, value)
				}
			}
		}
		
		// maybe overwrite this if subclass has specific fields
		open fun processIndidivualField(key: String, value: JsonNode) {}
		
		
		// abstract fun processIndividualUpdate(key: String, value: JsonNode)
		
		
		@JsonTypeName("swap")
		@Keep
		class Swap(
			symbol: String,
			@JsonDeserialize(using = BitmexCurrencyDeserializer::class) val positionCurrency: Currency,
			@JsonProperty("underlying") @JsonDeserialize(using = BitmexCurrencyDeserializer::class) underlyingCurrency: Currency,
			@JsonDeserialize(using = BitmexCurrencyDeserializer::class) quoteCurrency: Currency,
			pair: CurrencyPair = CurrencyPair(underlyingCurrency, quoteCurrency),
			@JacksonInject exchange: Exchange,
			
			state: String,
			@JsonDeserialize(using = TimestampDeserializer::class)
			@JsonSerialize(using = TimestampSerializer::class)
			listing: Instant? = null,
			@JsonDeserialize(using = TimestampDeserializer::class)
			@JsonSerialize(using = TimestampSerializer::class)
			settle: Instant? = null,
			markPrice: Double,
			openInterest: Long,
			volume24h: Long,
			volume: Long,
			vwap: Double,
			highPrice: Double,
			lowPrice: Double,
			lotSize: Double,
			tickSize: Double,
			isQuanto: Boolean,
			isInverse: Boolean,
			makerFee: Double,
			takerFee: Double,
			settlementFee: Double,
			capped: Boolean,
			openValue: Long,
			turnover24h: Long,
			indicativeSettlePrice: Double,
			limitDownPrice: Double? = null,
			limitUpPrice: Double? = null,
			@JsonDeserialize(using = TimestampDeserializer::class)
			@JsonSerialize(using = TimestampSerializer::class)
			var fundingTimestamp: Instant,
			var fundingRate: Double,
			var indicativeFundingRate: Double
		) :
			Bitmex(
				symbol,
				underlyingCurrency,
				quoteCurrency,
				pair,
				exchange,
				state,
				listing,
				settle,
				isQuanto,
				isInverse,
				capped,
				tickSize,
				lotSize,
				makerFee,
				takerFee,
				settlementFee,
				indicativeSettlePrice,
				markPrice,
				lowPrice,
				highPrice,
				turnover24h,
				openInterest,
				openValue,
				vwap,
				limitDownPrice,
				limitUpPrice,
				volume24h,
				volume
			) {
			
			@JsonIgnore override val dirString = "MEXS"
			
			@JsonIgnore override val periodLength: Duration = Duration.ofHours(8)
			
			override fun processIndidivualField(key: String, value: JsonNode) {
				when (key) {
					"fundingRate"           -> this@Swap.fundingRate = value.asDouble()
					"indicativeFundingRate" -> this@Swap.indicativeFundingRate = value.asDouble()
					// else                    -> println(key + " , " + value.toString())
				}
			}
			
			override fun equals(other: Any?): Boolean {
				if (this === other) return true
				if (javaClass != other?.javaClass) return false
				if (super.equals(other) == false) return false
				
				other as Swap
				if (fundingTimestamp != other.fundingTimestamp) return false
				return true
			}
			
			override fun hashCode(): Int {
				var result = super.hashCode()
				result = 31 * result + fundingTimestamp.hashCode()
				return result
			}
			
			
		}
		
		@JsonTypeName("future")
		@Keep
		class Future(
			symbol: String,
			@JsonDeserialize(using = BitmexCurrencyDeserializer::class) val positionCurrency: Currency,
			@JsonProperty("underlying") @JsonDeserialize(using = BitmexCurrencyDeserializer::class) underlyingCurrency: Currency,
			@JsonDeserialize(using = BitmexCurrencyDeserializer::class) quoteCurrency: Currency,
			pair: CurrencyPair = CurrencyPair(underlyingCurrency, quoteCurrency),
			@JacksonInject exchange: Exchange,
			
			state: String,
			@JsonDeserialize(using = TimestampDeserializer::class)
			@JsonSerialize(using = TimestampSerializer::class)
			override val listing: Instant,
			@JsonDeserialize(using = TimestampDeserializer::class)
			@JsonSerialize(using = TimestampSerializer::class)
			override val settle: Instant,
			markPrice: Double,
			openInterest: Long,
			volume24h: Long,
			volume: Long,
			vwap: Double,
			highPrice: Double,
			lowPrice: Double,
			lotSize: Double,
			tickSize: Double,
			isQuanto: Boolean,
			isInverse: Boolean,
			makerFee: Double,
			takerFee: Double,
			settlementFee: Double,
			capped: Boolean,
			openValue: Long,
			turnover24h: Long,
			indicativeSettlePrice: Double,
			limitDownPrice: Double? = null,
			limitUpPrice: Double? = null
		) :
			Bitmex(
				symbol,
				underlyingCurrency,
				quoteCurrency,
				pair,
				exchange,
				state,
				null,
				null,
				isQuanto,
				isInverse,
				capped,
				tickSize,
				lotSize,
				makerFee,
				takerFee,
				settlementFee,
				indicativeSettlePrice,
				markPrice,
				lowPrice,
				highPrice,
				turnover24h,
				openInterest,
				openValue,
				vwap,
				limitDownPrice,
				limitUpPrice,
				volume24h,
				volume
			) {
			
			@JsonIgnore override val periodLength: Duration = Duration.between(settle, listing)
			
			@JsonIgnore override val dirString = "MEXF"
			
			override fun equals(other: Any?): Boolean {
				if (this === other) return true
				if (javaClass != other?.javaClass) return false
				if (super.equals(other) == false) return false
				
				other as Future
				
				if (listing != other.listing) return false
				if (settle != other.settle) return false
				
				return true
			}
			
			override fun hashCode(): Int {
				var result = super.hashCode()
				result = 31 * result + listing.hashCode()
				result = 31 * result + settle.hashCode()
				return result
			}
			
		}
		
		@Keep
		sealed class Side(
			symbol: String,
			@JsonProperty("underlying") @JsonDeserialize(using = BitmexCurrencyDeserializer::class) underlyingCurrency: Currency,
			@JsonDeserialize(using = BitmexCurrencyDeserializer::class) quoteCurrency: Currency,
			pair: CurrencyPair = CurrencyPair(underlyingCurrency, quoteCurrency),
			@JacksonInject exchange: Exchange,
			
			state: String,
			markPrice: Double,
			openInterest: Long,
			volume24h: Long,
			volume: Long,
			vwap: Double,
			highPrice: Double,
			lowPrice: Double,
			lotSize: Double,
			tickSize: Double,
			isQuanto: Boolean,
			isInverse: Boolean,
			makerFee: Double,
			takerFee: Double,
			settlementFee: Double,
			capped: Boolean,
			openValue: Long,
			turnover24h: Long,
			indicativeSettlePrice: Double,
			limitDownPrice: Double? = null,
			limitUpPrice: Double? = null,
			@JsonDeserialize(using = TimestampDeserializer::class)
			@JsonSerialize(using = TimestampSerializer::class)
			override val listing: Instant,
			@JsonDeserialize(using = TimestampDeserializer::class)
			@JsonSerialize(using = TimestampSerializer::class)
			override val settle: Instant,
			val optionStrikePcnt: Double,
			val optionStrikeRound: Int,
			val optionStrikePrice: Double,
			val optionStrikeMultiplier: Double,
			var optionUnderlyingPrice: Double
		) :
			Bitmex(
				symbol,
				underlyingCurrency,
				quoteCurrency,
				pair,
				exchange,
				state,
				listing,
				settle,
				isQuanto,
				isInverse,
				capped,
				tickSize,
				lotSize,
				makerFee,
				takerFee,
				settlementFee,
				indicativeSettlePrice,
				markPrice,
				lowPrice,
				highPrice,
				turnover24h,
				openInterest,
				openValue,
				vwap,
				limitDownPrice,
				limitUpPrice,
				volume24h,
				volume
			) {
			
			override fun processIndidivualField(key: String, value: JsonNode) {
				when (key) {
					"optionUnderlyingPrice" -> this@Side.optionUnderlyingPrice = value.asDouble()
				}
			}
			
			override fun equals(other: Any?): Boolean {
				if (this === other) return true
				if (javaClass != other?.javaClass) return false
				if (super.equals(other) == false) return false
				
				other as Side
				
				if (listing != other.listing) return false
				if (settle != other.settle) return false
				if (optionStrikePcnt != other.optionStrikePcnt) return false
				if (optionStrikeRound != other.optionStrikeRound) return false
				if (optionStrikePrice != other.optionStrikePrice) return false
				
				return true
			}
			
			override fun hashCode(): Int {
				var result = super.hashCode()
				result = 31 * result + listing.hashCode()
				result = 31 * result + settle.hashCode()
				result = 31 * result + optionStrikePcnt.hashCode()
				result = 31 * result + optionStrikeRound
				result = 31 * result + optionStrikePrice.hashCode()
				return result
			}
			
			@JsonTypeName("down")
			@Keep
			class Down(
				symbol: String,
				@JsonProperty("underlying") @JsonDeserialize(using = BitmexCurrencyDeserializer::class) underlyingCurrency: Currency,
				@JsonDeserialize(using = BitmexCurrencyDeserializer::class) quoteCurrency: Currency,
				pair: CurrencyPair = CurrencyPair(underlyingCurrency, quoteCurrency),
				@JacksonInject exchange: Exchange,
				
				state: String,
				markPrice: Double,
				openInterest: Long,
				volume24h: Long,
				volume: Long,
				vwap: Double,
				highPrice: Double,
				lowPrice: Double,
				lotSize: Double,
				tickSize: Double,
				isQuanto: Boolean,
				isInverse: Boolean,
				makerFee: Double,
				takerFee: Double,
				settlementFee: Double,
				capped: Boolean,
				openValue: Long,
				turnover24h: Long,
				indicativeSettlePrice: Double,
				limitDownPrice: Double? = null,
				limitUpPrice: Double? = null,
				@JsonDeserialize(using = TimestampDeserializer::class)
				@JsonSerialize(using = TimestampSerializer::class)
				listing: Instant,
				@JsonDeserialize(using = TimestampDeserializer::class)
				@JsonSerialize(using = TimestampSerializer::class)
				settle: Instant,
				optionStrikePcnt: Double,
				optionStrikeRound: Int,
				optionStrikePrice: Double,
				optionStrikeMultiplier: Double,
				optionUnderlyingPrice: Double
			) :
				Side(
					symbol,
					underlyingCurrency,
					quoteCurrency,
					pair,
					exchange,
					state,
					markPrice,
					openInterest,
					volume24h,
					volume,
					vwap,
					highPrice,
					lowPrice,
					lotSize,
					tickSize,
					isQuanto,
					isInverse,
					makerFee,
					takerFee,
					settlementFee,
					capped,
					openValue,
					turnover24h,
					indicativeSettlePrice,
					limitDownPrice,
					limitUpPrice,
					listing,
					settle,
					optionStrikePcnt,
					optionStrikeRound,
					optionStrikePrice,
					optionStrikeMultiplier,
					optionUnderlyingPrice
				) {
				
				@JsonIgnore override val periodLength: Duration = Duration.between(listing, settle)
				
				@JsonIgnore val koBarrierPrice: Double = optionStrikePrice / 2
				
				@JsonIgnore override val dirString = "MEXD"
				
				override fun equals(other: Any?): Boolean {
					if (this === other) return true
					if (javaClass != other?.javaClass) return false
					if (super.equals(other) == false) return false
					
					other as Down
					
					if (koBarrierPrice != other.koBarrierPrice) return false
					
					return true
				}
				
				override fun hashCode(): Int {
					var result = super.hashCode()
					result = 31 * result + koBarrierPrice.hashCode()
					return result
				}
			}
			
			
			@JsonTypeName("up")
			@Keep
			class Up(
				symbol: String,
				@JsonProperty("underlying") @JsonDeserialize(using = BitmexCurrencyDeserializer::class) underlyingCurrency: Currency,
				@JsonDeserialize(using = BitmexCurrencyDeserializer::class) quoteCurrency: Currency,
				pair: CurrencyPair = CurrencyPair(underlyingCurrency, quoteCurrency),
				@JacksonInject exchange: Exchange,
				
				state: String,
				markPrice: Double,
				openInterest: Long,
				volume24h: Long,
				volume: Long,
				vwap: Double,
				highPrice: Double,
				lowPrice: Double,
				lotSize: Double,
				tickSize: Double,
				isQuanto: Boolean,
				isInverse: Boolean,
				makerFee: Double,
				takerFee: Double,
				settlementFee: Double,
				capped: Boolean,
				openValue: Long,
				turnover24h: Long,
				indicativeSettlePrice: Double,
				limitDownPrice: Double? = null,
				limitUpPrice: Double? = null,
				@JsonDeserialize(using = TimestampDeserializer::class)
				@JsonSerialize(using = TimestampSerializer::class)
				listing: Instant,
				@JsonDeserialize(using = TimestampDeserializer::class)
				@JsonSerialize(using = TimestampSerializer::class)
				settle: Instant,
				optionStrikePcnt: Double,
				optionStrikeRound: Int,
				optionStrikePrice: Double,
				optionStrikeMultiplier: Double,
				optionUnderlyingPrice: Double
			) :
				Side(
					symbol,
					underlyingCurrency,
					quoteCurrency,
					pair,
					exchange,
					state,
					markPrice,
					openInterest,
					volume24h,
					volume,
					vwap,
					highPrice,
					lowPrice,
					lotSize,
					tickSize,
					isQuanto,
					isInverse,
					makerFee,
					takerFee,
					settlementFee,
					capped,
					openValue,
					turnover24h,
					indicativeSettlePrice,
					limitDownPrice,
					limitUpPrice,
					listing,
					settle,
					optionStrikePcnt,
					optionStrikeRound,
					optionStrikePrice,
					optionStrikeMultiplier,
					optionUnderlyingPrice
				) {
				
				@JsonIgnore override val periodLength: Duration = Duration.between(listing, settle)
				
				@JsonIgnore override val dirString = "MEXU"
				
				override fun equals(other: Any?): Boolean {
					if (this === other) return true
					if (javaClass != other?.javaClass) return false
					if (super.equals(other) == false) return false
					
					other as Up
					
					if (listing != other.listing) return false
					if (settle != other.settle) return false
					
					return true
				}
				
				override fun hashCode(): Int {
					var result = super.hashCode()
					result = 31 * result + listing.hashCode()
					result = 31 * result + settle.hashCode()
					return result
				}
				
			}
			
			
		}
		
	}
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as Instrument
		
		if (symbol != other.symbol) return false
		if (exchange != other.exchange) return false
		
		return true
	}
	
	override fun hashCode(): Int {
		var result = pair.hashCode()
		result = 31 * result + exchange.hashCode()
		result = 31 * result + symbol.hashCode()
		return result
	}
	
}


