package tothrosch.json.rest

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.Currency
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.Instrument
import tothrosch.instrument.Instruments
import tothrosch.json.JsonException

sealed class InstrumentsRestDeserializer : StdDeserializer<Instruments>(Instruments::class.java) {
	
	override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Instruments {
		val jacksonNode: JsonNode = p?.codec?.readTree(p) ?: throw JsonException("cannot deserialize json because $p is null")
		return getInstruments(jacksonNode)
	}
	
	abstract fun getInstruments(node: JsonNode): Instruments
	
	
	object Binance : InstrumentsRestDeserializer() {
		
		private val maxInstruments = 50
		override fun getInstruments(node: JsonNode): Instruments {
			// first, sort all by volume
			// there are 5 different USD currencies: USDT, USDC, TUSD, BUSD and PAX
			val sortedByVolume: MutableList<Pair<Double, String>> = arrayListOf()
			for (symNode in node) {
				val sym = symNode.get("symbol").asText()
				if (hasUsdQuote(sym)) {
					sortedByVolume.add(Pair(symNode.get("quoteVolume").asDouble(), sym))
				}
				else {
					node.firstOrNull {
						val nestedSym = it.get("symbol").asText()
						nestedSym.startsWith(sym.takeLast(3)) && hasUsdQuote(nestedSym)
					}?.let {
							sortedByVolume.add(Pair(symNode.get("quoteVolume").asDouble() * it.get("lastPrice").asDouble(), sym))
					}
				}
			}
			sortedByVolume.sortByDescending { it.first }
			// then, take the 50 with the most volume and create the corresponding and instruments
			val instruments = sortedByVolume.take(maxInstruments).filter{ hasUsdBase(it.second) == false }.mapNotNull {
				val symbol = it.second
				if (hasUsdQuote(symbol)) {
					val base = if (it.second.takeLast(3) == "PAX") it.second.dropLast(3) else it.second.dropLast(4)
					symbolToInstrument(symbol, base, "USD", Exchange.BinanceExchange)
				}
				else {
					symbolToInstrument(symbol, symbol.dropLast(3), symbol.takeLast(3), Exchange.BinanceExchange)
				}
			}
			return Instruments(instruments.toSet())
		}
		
		
		/*{"symbol":"ETHBTC","priceChange":"-0.00035100","priceChangePercent":"-1.633","weightedAvgPrice":"0.02126209","prevClosePrice":"0.02149800","lastPrice":"0.02114700","lastQty":"0.42900000","bidPrice":"0.02114500","bidQty":"4.26500000","askPrice":"0.02114900","askQty":"0.00500000","openPrice":"0.02149800","highPrice":"0.02165000","lowPrice":"0.02100100","volume":"126268.06200000","quoteVolume":"2684.72304445","openTime":1571593061965,"closeTime":1571679461965,"firstId":147357768,"lastId":147457925,"count":100158}*/
		
		fun hasUsdQuote(sym: String) = when (sym.takeLast(4)) {
			"USDT", "USDC", "BUSD", "TUSD"  -> true
			else -> if (sym.takeLast(3) == "PAX") true else false
		}
		
		fun hasUsdBase(sym: String) = when (sym.take(4)) {
			"USDT", "USDC", "BUSD", "TUSD"  -> true
			else -> if (sym.take(3) == "PAX") true else false
		}
		
	}
	
	object Bitfinex : InstrumentsRestDeserializer() {
		
		private val maxInstruments = 30
		
		override fun getInstruments(node: JsonNode): Instruments {
			val instruments: MutableSet<Instrument> = mutableSetOf()
			val ethPrice = node.first { it.get("pair").toPrettyString() == "\"ETHUSD\"" }.get("last_price").asDouble()
			val btcPrice = node.first { it.get("pair").toPrettyString() == "\"BTCUSD\"" }.get("last_price").asDouble()
			val xmrPrice = node.first { it.get("pair").toPrettyString() == "\"XMRUSD\"" }.get("last_price").asDouble()
			val jpyPrice = 0.01
			// we can only subscribe to 30 instruments on websocket... fuck bitfinex
			// so we need to sort by USD volume and take the first 30
			val sorted = node.sortedByDescending {
				val volume = it.get("volume").asDouble()
				val price = it.get("last_price").asDouble()
				val usdVolume = volume * price
				val quote = it.get("pair").toPrettyString().takeLast(4).take(3)
				when (quote) {
					"ETH"   -> usdVolume*ethPrice
					"BTC"   -> usdVolume*btcPrice
					"XMR"   -> usdVolume*xmrPrice
					"JPY"   -> usdVolume*jpyPrice
					else    -> usdVolume
				}
			}
			var instrumentsAdded = 0
			pairloop@ for (symNode: JsonNode in sorted) {
				val symString = symNode.get("pair").asText()
				val cleanedSymString = symString.replace("\"","")
				val currencyPair: CurrencyPair? = try {
					if (cleanedSymString.length == 6) {
						val baseString = symToCur(cleanedSymString.take(3))
						val quoteString = symToCur(cleanedSymString.takeLast(3))
						CurrencyPair(baseString, quoteString)
					} else {
						val splittedString = cleanedSymString.split(":")
						val baseString = symToCur(splittedString[0])
						val quoteString = symToCur(splittedString[1])
						CurrencyPair(baseString, quoteString)
					}
				} catch (ex: Exception) {
					println("bitfinex: " + ex.localizedMessage)
					continue
				}
				if (instrumentsAdded == maxInstruments) break@pairloop
				currencyPair?.let {
					instruments.add(Instrument.Spot(it, Exchange.BitfinexExchange, symString))
					instrumentsAdded++
				}
			}
			
			return Instruments(instruments)
		}
		
		fun symToCur(sym: String) = when (sym) {
			"aio" -> Currency.AION
			"dsh" -> Currency.DASH
			"dat" -> Currency.DATA
			"iot" -> Currency.IOTA
			"ios" -> Currency.IOST
			"loo" -> Currency.LOOM
			"mna" -> Currency.MANA
			"qsh" -> Currency.QASH
			"qtm" -> Currency.QTUM
			"sng" -> Currency.SNGLS
			"spk" -> Currency.SPANK
			"ust" -> Currency.USD
			"yyw" -> Currency.YOYOW
			else  -> Currency.valueOf(sym.toUpperCase())
		}
		
	}
	
	object Bitmex : InstrumentsRestDeserializer() {
		
		override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Instruments {
			val jacksonArrayNode: ArrayNode = p?.codec?.readTree(p) ?: throw JsonException("cannot deserialize json because $p is null")
			val instruments = jacksonArrayNode.filter { it["state"].asText() == "Open" }.mapNotNull {
				when(it["typ"].asText()) {
					"FFWCSX"    -> p.codec.treeToValue(it, Instrument.Bitmex.Swap::class.java)
					"FFCCSX"    -> p.codec.treeToValue(it, Instrument.Bitmex.Future::class.java)
					"OCECCS"    -> p.codec.treeToValue(it, Instrument.Bitmex.Side.Up::class.java)
					"OPECCS"    -> p.codec.treeToValue(it, Instrument.Bitmex.Side.Down::class.java)
					else        -> null
				}
			}
			return Instruments(instruments.toSet())
		}
		
		override fun getInstruments(node: JsonNode): Instruments = Instruments(setOf())
	}
	
	
	object Bitstamp : InstrumentsRestDeserializer() {
		override fun getInstruments(node: JsonNode): Instruments {
			val instruments = node.filter { it.get("trading").asText() == "Enabled" }.mapNotNull {
				val symbol = it.get("url_symbol").asText()
				val baseQuoteSplit = it.get("name").asText().split("/")
				symbolToInstrument(symbol, baseQuoteSplit[0], baseQuoteSplit[1], Exchange.BitstampExchange)
			}
			return Instruments(instruments.toSet())
		}
	}
	
	
	object Coinbase : InstrumentsRestDeserializer() {
		override fun getInstruments(node: JsonNode): Instruments {
			val instruments = node.filter { it.get("status").asText() == "online" }.mapNotNull {
				val symbol = it.get("id").asText()
				val base = it.get("base_currency").asText().replace("USDC", "USD")
				val quote = it.get("quote_currency").asText().replace("USDC", "USD")
				symbolToInstrument(symbol, base, quote, Exchange.CoinbaseExchange)
			}
			return Instruments(instruments.toSet())
		}
	}
	
	object Gemini : InstrumentsRestDeserializer() {
		override fun getInstruments(node: JsonNode): Instruments {
			val instruments = node.mapNotNull {
				val symbol = it.asText()
				val base = symbol.take(3).toUpperCase()
				val quote = symbol.takeLast(3).toUpperCase()
				symbolToInstrument(symbol, base, quote, Exchange.GeminiExchange)
			}
			return Instruments(instruments.toSet())
		}
	}
	
	
	object Kraken : InstrumentsRestDeserializer() {
		override fun getInstruments(node: JsonNode): Instruments {
			val instruments: Set<Instrument> = node.get("result").filter { it.has("wsname") }.mapNotNull {
				val wsname = it.get("wsname").asText()
				val wsnameSplit = wsname.split("/")
				val base = wsnameSplit[0].replaceSyms()
				val quote = wsnameSplit[1].replaceSyms()
				symbolToInstrument(wsname, base, quote, Exchange.KrakenExchange)
			}.toSet()
			return Instruments(instruments)
		}
		
		private fun String.replaceSyms() = this.replace("XBT", "BTC").replace("USDT", "USD").replace("XDG", "DOGE")
	}
	
	
	
	
	/*object Poloniex : InstrumentsRestDeserializer() {
		override fun getInstruments(node: JsonNode): Instruments {
			val instruments = node.fieldNames().asSequence().mapNotNull {
				symbolToInstrument(
					symbol = it,
					base = it.substringBefore("_").replace("USDT", "USD"),
					quote = it.substringBefore("_").replace("USDT", "USD"),
					exchange = Exchange.PoloniexExchange
				)
			}
			return Instruments(instruments.toSet())
		}
	}*/
	
	fun symbolToInstrument(symbol: String, base: String, quote: String, exchange: Exchange): Instrument? {
			try {
				val baseCurrency: Currency = Currency.valueOf(base)
				val quoteCurrency: Currency = Currency.valueOf(quote)
				return Instrument.Spot(CurrencyPair(baseCurrency, quoteCurrency), exchange, symbol)
			} catch (ex: Exception) {
				println("${exchange.name}: " + ex.localizedMessage)
				return null
			}
	}
	
}