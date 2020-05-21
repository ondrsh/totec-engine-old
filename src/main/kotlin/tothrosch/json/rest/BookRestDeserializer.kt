package tothrosch.json.rest

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import tothrosch.engine.Keep
import tothrosch.instrument.book.*
import tothrosch.json.JsonException
import tothrosch.settings.Settings
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

/**
 * Created by ndrsh on 27.06.17.
 */

sealed class BookRestDeserializer : StdDeserializer<Book> {
	
	constructor(vc: Class<Any>?) : super(vc)
	constructor() : this(null)
	
	override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Book {
		val jacksonJsonNode: JsonNode = p?.codec?.readTree(p) ?: throw JsonException("cannot deserialize json because $p is null")
		return getBook(jacksonJsonNode)
	}
	
	abstract fun getBook(node: JsonNode): Book
	
	object Binance : BookRestDeserializer() {
		// {"lastUpdateId":1213303593,"bids":[["8195.99000000","0.21611100"],["8195.98000000","0.03293800"],["8195.89000000","0.04241800"],["8195.68000000","0.25000000"],["8195.12000000","0.08345600"],["8194.83000000","0.10000000"],["8194.15000000","0.06469800"],["8194.14000000","1.25056600"],["8194.13000000","0.03175300"],["8194.12000000","0.03306000"]],"asks":[["8196.97000000","0.09452400"],["8196.99000000","0.70580200"],["8197.00000000","3.80211600"],["8197.01000000","0.18111200"],["8197.03000000","0.01693400"],["8197.98000000","1.00000000"],["8197.99000000","1.95355500"],["8198.87000000","0.03549500"],["8198.88000000","0.79862600"],["8198.90000000","200.22273600"]]}
		
		override fun getBook(node: JsonNode): Book {
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("bids")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(0).asDouble().toString() to BookEntry(      price = it.get(0).asDouble(),
				                                                                                                            amount = it.get(1).asDouble(),
				                                                                                                            id = it.get(0).asDouble().toString())
				                                                  }
				                                                  .toMap())
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("asks")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(0).asDouble().toString() to BookEntry(      price = it.get(0).asDouble(),
				                                                                                                            amount = it.get(1).asDouble(),
				                                                                                                            id = it.get(0).asDouble().toString())
				                                                  }
				                                                  .toMap())
			
			val sequence = node.get("lastUpdateId").asLong()
			
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(bidsMutable = BidsMutable(bidsMap, bidsSet),
			            asksMutable = AsksMutable(asksMap, asksSet),
			            sequence =  sequence)
		}
		
		// {"bids":[{"price":"2370.9","amount":"1.754512","timestamp":"1499698587.0"},{"price":"2370.8","amount":"9.26671092","timestamp":"1499698583.0"},...,{"price":"0.000011","amount":"1271.4409","timestamp":"1470914781.0"}],"asks":[{"price":"2373.8","amount":"1.075","timestamp":"1499698568.0"},{"price":"2374.0","amount":"2.0","timestamp":"1499698579.0"},...,{"price":"13615000.0","amount":"0.06","timestamp":"1499633976.0"},{"price":"99999000.0","amount":"0.1","timestamp":"1494390581.0"}]}
	}
	
	object Bithumb : BookRestDeserializer() {
		
		// {"status":"0000","data":{"timestamp":"1510793455719","payment_currency":"KRW","BTC":{"order_currency":"BTC","bids":[{"quantity":"0.58930000","price":"8240000"},{"quantity":"0.26050000","price":"8239000"},{"quantity":"0.20000000","price":"8236000"},{"quantity":"4.60775567","price":"8235000"},{"quantity":"0.05300000","price":"8234000"}],"asks":[{"quantity":"2.07510000","price":"8241000"},{"quantity":"1.28240798","price":"8242000"},{"quantity":"0.28990417","price":"8244000"},{"quantity":"0.69760000","price":"8245000"},{"quantity":"1.00000000","price":"8246000"}]},"ETH":{"order_currency":"ETH","bids":[{"quantity":"16.14790000","price":"377150"},{"quantity":"6.94370000","price":"377100"},{"quantity":"101.07778654","price":"377050"},{"quantity":"6.84720000","price":"376800"},{"quantity":"10.00000000","price":"376700"}],"asks":[{"quantity":"0.07130000","price":"377200"},{"quantity":"0.17860000","price":"377600"},{"quantity":"10.20000000","price":"377750"},{"quantity":"27.44900000","price":"377900"},{"quantity":"9.45010000","price":"378000"}]},"DASH":{"order_currency":"DASH","bids":[{"quantity":"10.03940000","price":"479500"},{"quantity":"6.99120000","price":"479450"},{"quantity":"7.75843738","price":"479400"},{"quantity":"4.60000000","price":"479000"},{"quantity":"66.88043125","price":"478950"}],"asks":[{"quantity":"0.30000000","price":"479950"},{"quantity":"1.59860000","price":"480000"},{"quantity":"2.09690000","price":"480200"},{"quantity":"1.31800000","price":"480600"},{"quantity":"1.71500000","price":"480700"}]},"LTC":{"order_currency":"LTC","bids":[{"quantity":"81.07808720","price":"72670"},{"quantity":"844.97630000","price":"72660"},{"quantity":"55.38114297","price":"72600"},{"quantity":"8.33970000","price":"72520"},{"quantity":"1.76620000","price":"72510"}],"asks":[{"quantity":"8.95730000","price":"72680"},{"quantity":"6.56000000","price":"72690"},{"quantity":"564.06420000","price":"72720"},{"quantity":"20.00000000","price":"72850"},{"quantity":"100.00000000","price":"72880"}]},"ETC":{"order_currency":"ETC","bids":[{"quantity":"115.40098729","price":"19535"},{"quantity":"2.00000000","price":"19510"},{"quantity":"500.98120000","price":"19505"},{"quantity":"4265.33740000","price":"19500"},{"quantity":"7.04750000","price":"19495"}],"asks":[{"quantity":"200.31040000","price":"19540"},{"quantity":"180.28450000","price":"19545"},{"quantity":"1467.05240000","price":"19550"},{"quantity":"58.82800000","price":"19555"},{"quantity":"79.31800000","price":"19560"}]},"XRP":{"order_currency":"XRP","bids":[{"quantity":"705309.47814800","price":"244"},{"quantity":"1724477.81760000","price":"243"},{"quantity":"1669918.93460000","price":"242"},{"quantity":"1475760.97598700","price":"241"},{"quantity":"1122774.38660000","price":"240"}],"asks":[{"quantity":"4755734.11770600","price":"245"},{"quantity":"1957270.99200000","price":"246"},{"quantity":"2590389.24870000","price":"247"},{"quantity":"2580722.33970000","price":"248"},{"quantity":"2645576.04210000","price":"249"}]},"BCH":{"order_currency":"BCH","bids":[{"quantity":"5.73030000","price":"1339100"},{"quantity":"1.26660000","price":"1339000"},{"quantity":"5.60000000","price":"1338300"},{"quantity":"3.71370000","price":"1338200"},{"quantity":"16.12000000","price":"1338100"}],"asks":[{"quantity":"6.35333234","price":"1339300"},{"quantity":"4.59850000","price":"1339400"},{"quantity":"12.54400000","price":"1340000"},{"quantity":"29.22255179","price":"1340100"},{"quantity":"5.42078806","price":"1340700"}]},"XMR":{"order_currency":"XMR","bids":[{"quantity":"103.62140000","price":"137150"},{"quantity":"88.51490000","price":"137120"},{"quantity":"3.00000000","price":"137110"},{"quantity":"20.30030000","price":"137100"},{"quantity":"2.10010000","price":"137080"}],"asks":[{"quantity":"19.11666489","price":"137220"},{"quantity":"36.40000000","price":"137490"},{"quantity":"26.92415273","price":"137500"},{"quantity":"63.82650000","price":"137520"},{"quantity":"2.13070000","price":"137530"}]},"ZEC":{"order_currency":"ZEC","bids":[{"quantity":"9.80000000","price":"335200"},{"quantity":"7.54771692","price":"335150"},{"quantity":"2.20000000","price":"335100"},{"quantity":"23.03090000","price":"335050"},{"quantity":"198.04270000","price":"335000"}],"asks":[{"quantity":"5.00000000","price":"335950"},{"quantity":"12.85310000","price":"336000"},{"quantity":"2.00010000","price":"336250"},{"quantity":"1.00000000","price":"336400"},{"quantity":"16.09000000","price":"336500"}]},"QTUM":{"order_currency":"QTUM","bids":[{"quantity":"76.60510000","price":"13020"},{"quantity":"126.00000000","price":"13010"},{"quantity":"795.86910000","price":"13005"},{"quantity":"5958.29198733","price":"13000"},{"quantity":"250.47813077","price":"12995"}],"asks":[{"quantity":"838.74400000","price":"13025"},{"quantity":"312.38460000","price":"13040"},{"quantity":"1061.12340000","price":"13045"},{"quantity":"1190.87200000","price":"13050"},{"quantity":"300.00000000","price":"13055"}]}}}
		
		override fun getBook(node: JsonNode): Book {
			val time = node.get("time").asLong()
			
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("bids")
				                                                  .take(Settings.bookArraySize)
				                                                  .map {
					                                                  it.get("price").asDouble().toString() to BookEntry(
						                                                  it.get("price").asDouble(),
						                                                  it.get("quantity").asDouble(),
						                                                  it.get("price").asDouble().toString(),
						                                                  time
					                                                  )
				                                                  }
				                                                  .toMap())
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("asks")
				                                                  .take(Settings.bookArraySize)
				                                                  .map {
					                                                  it.get("price").asDouble().toString() to BookEntry(
						                                                  it.get("price").asDouble(),
						                                                  it.get("quantity").asDouble(),
						                                                  it.get("price").asDouble().toString(),
						                                                  time
					                                                  )
				                                                  }
				                                                  .toMap())
			
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet))
		}
	}
	
	
	object Bitfinex : BookRestDeserializer() {
		
		override fun getBook(node: JsonNode): Book {
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("bids")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get("price").asDouble().toString() to BookEntry(it.get("price").asDouble(), it.get("amount").asDouble(), it.get("price").asDouble().toString(), (it.get("timestamp").asDouble() * 1000).toLong()) }
				                                                  .toMap())
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("asks")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get("price").asDouble().toString() to BookEntry(it.get("price").asDouble(), it.get("amount").asDouble(), it.get("price").asDouble().toString(), (it.get("timestamp").asDouble() * 1000).toLong()) }
				                                                  .toMap())
			
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet))
		}
		
		// {"bids":[{"price":"2370.9","amount":"1.754512","timestamp":"1499698587.0"},{"price":"2370.8","amount":"9.26671092","timestamp":"1499698583.0"},...,{"price":"0.000011","amount":"1271.4409","timestamp":"1470914781.0"}],"asks":[{"price":"2373.8","amount":"1.075","timestamp":"1499698568.0"},{"price":"2374.0","amount":"2.0","timestamp":"1499698579.0"},...,{"price":"13615000.0","amount":"0.06","timestamp":"1499633976.0"},{"price":"99999000.0","amount":"0.1","timestamp":"1494390581.0"}]}
	}
	
	object Bitmex : BookRestDeserializer() {
		
		override fun getBook(node: JsonNode): Book {
			val bidsMap: HashMap<String, BookEntry> = HashMap<String, BookEntry>(node
				                                                                     .filter { it.get("side").asText() == "Buy" }
				                                                                     .take(Settings.bookArraySize)
				                                                                     .map { it.get("id").asText() to BookEntry(it.get("price").asDouble(), it.get("size").asDouble(), it.get("id").asText()) }
				                                                                     .toMap())
			
			
			val asksMap: HashMap<String, BookEntry> = HashMap<String, BookEntry>(node
				                                                                     .filter { it.get("side").asText() == "Sell" }
				                                                                     .take(Settings.bookArraySize)
				                                                                     .map { it.get("id").asText() to BookEntry(it.get("price").asDouble(), it.get("size").asDouble(), it.get("id").asText()) }
				                                                                     .toMap())
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			val book = Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet))
			return book
		}
	}
	
	
	object Bitstamp : BookRestDeserializer() {
		
		override fun getBook(node: JsonNode): Book {
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("bids")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(0).asDouble().toString() to BookEntry(it.get(0).asDouble(), it.get(1).asDouble(), it.get(0).asDouble().toString()) }
				                                                  .toMap())
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("asks")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(0).asDouble().toString() to BookEntry(it.get(0).asDouble(), it.get(1).asDouble(), it.get(0).asDouble().toString()) }
				                                                  .toMap())
			
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet), sequence = -1)
		}
		
		
		// {"timestamp": "1499957821", "bids": [["2078.00", "1.19739842"], ["2077.26", "3.00000000"], (...) ["0.02", "1000.00000000"], ["0.01", "500.00000000"]], "asks": [["2087.11", "0.14800000"], ["2087.71", "1.25824099"], (...) ["500000.00", "0.49526529"], ["1000000.00", "1.73757564"]]}
	}
	
	object Coinbase : BookRestDeserializer() {
		
		
		override fun getBook(node: JsonNode): Book {
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("bids")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(2).asText() to BookEntry(it.get(0).asDouble(), it.get(1).asDouble(), it.get(2).asText()) }
				                                                  .toMap())
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("asks")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(2).asText() to BookEntry(it.get(0).asDouble(), it.get(1).asDouble(), it.get(2).asText()) }
				                                                  .toMap())
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet), sequence = node.get("sequence").asLong())
		}
	}
	
	
	object Gemini : BookRestDeserializer() {
		
		override fun getBook(node: JsonNode): Book {
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("bids")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get("price").asText() to BookEntry(it.get("price").asDouble(), it.get("amount").asDouble(), it.get("price").asText()) }
				                                                  .toMap())
			
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("asks")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get("price").asText() to BookEntry(it.get("price").asDouble(), it.get("amount").asDouble(), it.get("price").asText()) }
				                                                  .toMap())
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet))
			
		}
		// {"bids":[{"price":"2759.16","amount":"10.20355558","timestamp":"1500936888"},{"price":"2758.70","amount":"0.1807","timestamp":"1500936888"},{"price":"2757.98","amount":"1.2267","timestamp":"1500936888"},{"price":"2757.00","amount":"0.00906782","timestamp":"1500936888"},{"price":"2756.90","amount":"9.5834","timestamp":"1500936888"}],"asks":[{"price":"2759.17","amount":"7.34542978","timestamp":"1500936888"},{"price":"2759.33","amount":"3.93259513","timestamp":"1500936888"},{"price":"2759.47","amount":"4.85","timestamp":"1500936888"},{"price":"2759.75","amount":"7.7","timestamp":"1500936888"},{"price":"2759.99","amount":"25.64012071","timestamp":"1500936888"}]}
	}
	
	object Kraken : BookRestDeserializer() {
		
		override fun getBook(node: JsonNode): Book {
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("result")
				                                                  .first()
				                                                  .get("bids")
				                                                  .map {
					                                                  val price = it[0].asDouble()
					                                                  val id = price.toString()
					                                                  val amount = it[1].asDouble()
					                                                  val time = it[2].asLong()
					                                                  id to BookEntry(price, amount, id, time) }
				                                                  .toMap())
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
			                                                  .get("result")
			                                                  .first()
			                                                  .get("asks")
			                                                  .map {
				                                                  val price = it[0].asDouble()
				                                                  val id = price.toString()
				                                                  val amount = it[1].asDouble()
				                                                  val time = it[2].asLong()
				                                                  id to BookEntry(price, amount, id, time) }
			                                                  .toMap())
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet), sequence = max(bidsSet.map { it.time }.max()!!, asksSet.map { it.time }.max()!!))
		}
	}
	
	object Poloniex : BookRestDeserializer() {
		
		override fun getBook(node: JsonNode): Book {
			val seq: Long = node.get("seq").asLong()
			
			
			val bidsMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("bids")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(0).asDouble().toString() to BookEntry(it.get(0).asDouble(), it.get(1).asDouble(), it.get(0).asDouble().toString()) }
				                                                  .toMap())
			
			val asksMap: HashMap<String, BookEntry> = HashMap(node
				                                                  .get("asks")
				                                                  .take(Settings.bookArraySize)
				                                                  .map { it.get(0).asDouble().toString() to BookEntry(it.get(0).asDouble(), it.get(1).asDouble(), it.get(0).asDouble().toString()) }
				                                                  .toMap())
			
			
			val bidsSet: TreeSet<BookEntry> = TreeSet(bidsMap.values.toSortedSet(BidsComparator))
			val asksSet: TreeSet<BookEntry> = TreeSet(asksMap.values.toSortedSet(AsksComparator))
			
			return Book(BidsMutable(bidsMap, bidsSet), AsksMutable(asksMap, asksSet), sequence = seq)
		}
		
		// {"asks":[["0.00534081",0.29666555],["0.00534082",23.64750379],...,["0.03468676",1.81710545],["0.03487865",2.46364]],"bids":[["0.00523182",0.66018909],["0.00522950",0.02207835],...,["0.00000002",50000000],["0.00000001",1822363]],"isFrozen":"0","seq":17469376}
	}
}