package tothrosch.exchange.currencies.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import tothrosch.exchange.currencies.Currency
import tothrosch.util.execute
import tothrosch.util.isError
import java.time.LocalDate

object FixerApi {

	val baseUrl = "http://data.fixer.io/api/"
	val client: CloseableHttpAsyncClient = HttpAsyncClients.createDefault()
	val mapper: ObjectMapper = ObjectMapper()


	init {
		client.start()
	}


	suspend fun getByDate(localDate: LocalDate): HashMap<Currency, Double> {
		try {
			val queryCurrencies = Currency.values().filter { it.isFiat }
			val key = "access_key=10dbc09feed6b16b75c4c807dddd21b7"
			val query = "symbols=${queryCurrencies.joinToString(",")}"
			val response = client.execute(HttpGet(baseUrl + "$localDate" + "?" + key + "&" + query))
			if (response.statusLine.statusCode.isError) return HashMap()
			val jsonNode: JsonNode = mapper.readValue(response.entity.content, JsonNode::class.java)
			val jsonRates = jsonNode.get("rates")
			val multipliers: HashMap<Currency, Double> = hashMapOf()
			val eurUsdMultiplier = jsonRates.get("USD").asDouble()
			queryCurrencies.filter { it != Currency.USD }.forEach { multipliers.put(it, eurUsdMultiplier / jsonRates.get(it.toString()).asDouble()) }
			return multipliers
		} catch (ex: Exception) {
			return HashMap()
		}

	}


}