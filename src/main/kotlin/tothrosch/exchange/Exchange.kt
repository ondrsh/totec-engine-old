package tothrosch.exchange

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import tothrosch.engine.Global
import tothrosch.engine.Keep
import tothrosch.engine.message.Message
import tothrosch.engine.message.Request
import tothrosch.engine.mode
import tothrosch.exchange.currencies.Currency
import tothrosch.instrument.Instrument
import tothrosch.instrument.Instruments
import tothrosch.instrument.database.reader.DataReader
import tothrosch.instrument.database.reader.InstrumentReader
import tothrosch.json.rest.BookRestDeserializer
import tothrosch.json.rest.InstrumentsRestDeserializer
import tothrosch.networking.livefeed.LiveFeed
import tothrosch.networking.livefeed.adapter.*
import tothrosch.networking.rest.RestClient
import tothrosch.networking.rest.adapter.binance.BinanceRestClient
import tothrosch.networking.rest.adapter.bitfinex.BitfinexRestClient
import tothrosch.networking.rest.adapter.bitmex.BitmexRestClient
import tothrosch.networking.rest.adapter.bitstamp.BitstampRestClient
import tothrosch.networking.rest.adapter.gdax.CoinbaseRestClient
import tothrosch.networking.rest.adapter.gemini.GeminiRestClient
import tothrosch.networking.rest.adapter.kraken.KrakenRestClient
import tothrosch.settings.Settings
import tothrosch.util.Directory
import tothrosch.util.logLive
import java.io.File
import java.io.FileFilter
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

/**
 * Created by ndrsh on 29.06.17.
 */

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY,
	property = "type"
)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
sealed class Exchange: CoroutineScope {
	abstract val name: Name
	override val coroutineContext: CoroutineContext = Settings.appContext // Dispatchers.Default //("$name ThreadContext")
	abstract val restInitNecessary: Boolean // only at Coinbase and Binance
	// initialization order is important in subclasses
	// first, deserializers have to be initialized, otherwise Restclient can't add them to the jackson modules
	abstract val bookRestDeserializer: BookRestDeserializer
	abstract val instrumentsRestDeserializer: InstrumentsRestDeserializer
	abstract val restClient: RestClient
	abstract val liveFeed: LiveFeed
	lateinit var instruments: Set<Instrument> // by lazy { loadInstrumentsLazily() }
	val symToInst: HashMap<String, Instrument> = hashMapOf()
	val nonce: AtomicLong = AtomicLong(System.currentTimeMillis())
	val mapper = jacksonObjectMapper()
	
	// abstract fun pairToSym(pair: CurrencyPair): String
	
	fun initialize() {
		instruments = if (mode.isLive) {
			restClient.httpAsyncClient.start()
			getLiveInstruments()
		} else getReadInstruments()
		instruments.forEach { addInstrument(it) }
		if (mode.isLive) liveFeed.job.start()
	}
	
	private fun addInstrument(instrument: Instrument) = symToInst.put(instrument.symbol, instrument)
	
	private fun filterInstrument(instrument: Instrument): Boolean {
		// if (instrument.symbol != "XBTUSD") return false
		if (Global.startPairs.isEmpty() && Global.startCurrencies.isEmpty()) return true
		// TODO implement filtering on currencies also... but take care that Trading handles this right
		if (Global.startPairs.contains(instrument.pair)) return true
		return false
	}
	
	
	// probably give directory, so Instruments.fromString can work with text files that specify more information
	private fun getReadInstruments(): Set<Instrument> {
		val path: String = Settings.dataPath + File.separator + name.toString().toLowerCase() + File.separator
		val dirs: Array<Directory> = File(path).listFiles(FileFilter { it.isDirectory }) ?: arrayOf()
		
		val instruments = mutableSetOf<Instrument>()
		for (dir: Directory in dirs) {
			try {
				InstrumentReader(dir,
				                 DataReader.broadCastChannel,
				                 DataReader.feedbackChannel,
				                 coroutineContext).apply {
					if (filterInstrument(instrument)) {
						DataReader.instrumentReaders.add(this)
						instruments.add(instrument)
					}
				}
			} catch (ex: ExceptionInInitializerError) {
				null
			}
		}
		return instruments
	}
	
	open fun getLiveInstruments(): Set<Instrument> {
		val instrumentsChannel: Channel<Message<Instruments>> = Channel(1)
		restClient.executeRequest(restClient.getActiveInstrumentsHttpRequest(), Request(returnChannel = instrumentsChannel,
		                                                                                content = Instruments()))
		val instrumentsMessage: Message<Instruments> = runBlocking { instrumentsChannel.receive() }
		if (instrumentsMessage.content.size == 0) {
			logLive("Couldn't get live instruments at exchange ${this.name}")
		}
		return instrumentsMessage.content.filter { filterInstrument(it) }.toSet()
	}
	
	override fun toString(): String {
		return name.toString()
	}
	
	@JsonTypeName("binance")
	@Keep
	object BinanceExchange : Exchange() {
		override val name = Name.BINANCE
		override val restInitNecessary = true
		override val bookRestDeserializer = BookRestDeserializer.Binance
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Binance
		override val restClient = BinanceRestClient
		override val liveFeed = BinanceWebsocket()
		
		
		// is never actually used, because
		// 0) when live, symbol gets set by REST
		// 1) when backtesting, symbol gets set by instrument.companion.fromString()
/*		override fun pairToSym(pair: CurrencyPair): String {
			val baseSymbol = pair.base.toString().replace("BTC", "XBT", true)
			val quoteSymbol = pair.quote.toString().replace("BTC", "XBT", true)
			return baseSymbol + quoteSymbol
		}*/
	}
	
	
	
	/*@JsonTypeName("bithumb")
	object BithumbExchange : Exchange() {
		override val name = Name.BITHUMB
		override val restInitNecessary = false
		override val restClient = BithumbRestClient(this)
		override val liveFeed = BithumbLivefeed(this)
		
		override fun pairToSym(pair: CurrencyPair) = "${pair.base}"
		override fun getLiveInstruments() = setOf(
			Instrument.Spot(CurrencyPair(Currency.BTC, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.BCH, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.DASH, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.ETH, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.ETC, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.LTC, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.QTUM, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.XMR, Currency.KRW), this),
			Instrument.Spot(CurrencyPair(Currency.ZEC, Currency.KRW), this)
		)
	}*/
	
	
	
	
	@JsonTypeName("bitfinex")
	@Keep
	object BitfinexExchange : Exchange() {
		override val name = Name.BITFINEX
		override val restInitNecessary = false
		override val bookRestDeserializer = BookRestDeserializer.Bitfinex
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Bitfinex
		override val restClient = BitfinexRestClient
		override val liveFeed = BitfinexWebsocket
		
	/*	override fun pairToSym(pair: CurrencyPair): String {
			val baseSym = curToSym(pair.base)
			val quoteSym = curToSym(pair.quote)
			return baseSym + quoteSym
		}*/
		
		// override fun symToPair(sym: String) = CurrencyPair(symToCur(sym.take(3)), symToCur(sym.takeLast(3)))
		
		fun symToCur(sym: String) = when (sym) {
			"dsh" -> Currency.DASH
			"dat" -> Currency.DATA
			"iot" -> Currency.IOTA
			"mna" -> Currency.MANA
			"qsh" -> Currency.QASH
			"qtm" -> Currency.QTUM
			"sng" -> Currency.SNGLS
			"spk" -> Currency.SPANK
			"yyw" -> Currency.YOYOW
			else  -> Currency.valueOf(sym.toUpperCase())
		}
		
		fun curToSym(currency: Currency) = when (currency) {
			Currency.DASH  -> "DSH"
			Currency.DATA  -> "DAT"
			Currency.IOTA  -> "IOT"
			Currency.MANA  -> "MNA"
			Currency.QASH  -> "QSH"
			Currency.QTUM  -> "QTM"
			Currency.SNGLS -> "SNG"
			Currency.SPANK -> "SPK"
			Currency.YOYOW -> "YYW"
			else           -> currency.toString().toUpperCase()
		}
	}
	
	@Keep
	@JsonTypeName("bitmex")
	object BitmexExchange : Exchange() {
		override val name = Name.BITMEX
		override val restInitNecessary = false
		override val bookRestDeserializer = BookRestDeserializer.Bitmex
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Bitmex
		override val restClient = BitmexRestClient
		override val liveFeed = BitmexWebsocket()
		
		
		// is never actually used, because
		// 1) when live, symbol gets set by REST
		// 2) when backtesting, symbol gets set by instrument.companion.fromString()
	/*	override fun pairToSym(pair: CurrencyPair): String {
			val baseSymbol = pair.base.toString().replace("BTC", "XBT", true)
			val quoteSymbol = pair.quote.toString().replace("BTC", "XBT", true)
			return baseSymbol + quoteSymbol
		}*/
	}
	
	
	@JsonTypeName("bitstamp")
	@Keep
	object BitstampExchange : Exchange() {
		override val name = Name.BITSTAMP
		override val restInitNecessary = false
		override val bookRestDeserializer = BookRestDeserializer.Bitstamp
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Bitstamp
		override val restClient = BitstampRestClient
		override val liveFeed = BitstampWebsocket
		
		// override fun pairToSym(pair: CurrencyPair) = "${pair.base.toString().toLowerCase()}${pair.quote.toString().toLowerCase()}"
		
		
	}
	
	@JsonTypeName("coinbase")
	@Keep
	object CoinbaseExchange : Exchange() {
		override val name = Name.COINBASE
		override val restInitNecessary = true
		override val bookRestDeserializer = BookRestDeserializer.Coinbase
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Coinbase
		override val restClient = CoinbaseRestClient
		override val liveFeed = CoinbaseWebsocket
		
		//override fun pairToSym(pair: CurrencyPair) = "${pair.base}-${pair.quote}"
	}
	
	@JsonTypeName("gemini")
	@Keep
	object GeminiExchange : Exchange() {
		override val name = Name.GEMINI
		override val restInitNecessary = false
		override val bookRestDeserializer = BookRestDeserializer.Gemini
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Gemini
		override val restClient = GeminiRestClient
		override val liveFeed = GeminiLivefeed
	}
	
	/*	object OkexExchange: Exchange() {
		override val name = Name.OKEX
		override val restInitNecessary = false
		override val restClient = OkexRestClient(this)
		override val liveFeed = OkexWebsocket(this)

		override fun pairToSym(pair: CurrencyPair) = "TODO"

		override fun getLiveInstruments(): Set<Instrument> {
			val instrumentsChannel: Channel<Message<Instruments>> = Channel(1)
			launch(threadContext) { restClient.instrumentHandler.handleRequest(Request(instrumentsChannel)) }
			val instrumentsMessage: Message<Instruments> = runBlocking { instrumentsChannel.receive() }
			if (instrumentsMessage.content != null && instrumentsMessage.content.size > 0) {
				return instrumentsMessage.content
			} else {
				return Instruments()
			}
		}
	}*/
	
	
	@JsonTypeName("kraken")
	@Keep
	object KrakenExchange : Exchange() {
		override val name = Name.KRAKEN
		override val bookRestDeserializer = BookRestDeserializer.Kraken
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Kraken
		override val restInitNecessary = false
		override val restClient = KrakenRestClient
		override val liveFeed = KrakenWebsocket
}
	
	/*@JsonTypeName("poloniex")
	object PoloniexExchange : Exchange() {
		override val name = Name.POLONIEX
		override val bookRestDeserializer = BookRestDeserializer.Poloniex
		override val instrumentsRestDeserializer = InstrumentsRestDeserializer.Poloniex
		override val restInitNecessary = false
		override val restClient = PoloniexRestClient
		override val liveFeed = PoloniexWebsocket(this)
		
	*//*	override fun pairToSym(pair: CurrencyPair) =
			"${pair.quote.toString().replace("USD", "USDT")}_${pair.base.toString().replace("USD", "USDT")}"*//*
		
	}*/
	
	
	@Keep
	enum class Name {
		BINANCE,
		BITHUMB,
		BITFINEX,
		BITMEX,
		BITSTAMP,
		COINBASE,
		GEMINI,
		KRAKEN,
		// OKEX,
		POLONIEX;
		
		override fun toString(): String {
			return super.toString().toLowerCase()
		}
		
	}
}

