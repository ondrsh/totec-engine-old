/*
package tothrosch.ml.features

import tothrosch.engine.globalInstruments
import tothrosch.exchange.Exchange
import tothrosch.exchange.currencies.Currency
import tothrosch.exchange.currencies.CurrencyPair
import tothrosch.instrument.Instrument
import tothrosch.ml.features.global.GlobalFeatures
import tothrosch.util.getFurthestBookEntry


class BitmexFeatureMlpStrategy(instrument: Instrument): Indicators(instrument) {

	val otherInstruments: List<Instrument> = listOf(
			globalInstruments.filter { it.exchange.impl == Exchange.Impl.GDAX }[0],
			globalInstruments.filter { it.exchange.impl == Exchange.Impl.BITSTAMP }[0],
			globalInstruments.filter { it.exchange.impl == Exchange.Impl.BITFINEX }[0]
			// globalInstruments.filter { it.exchange.impl == Exchange.Impl.GEMINI }[0]
			//globalInstruments.filter { it.exchange.impl == Exchange.Impl.POLONIEX }[0]
	)


	var isInitialized = false

	override fun secondFlush() {

		       if (!isInitialized) {
				   gdax_BTCUSD = globalInstruments.filter { it.exchange.impl == Exchange.Impl.GDAX && it.pair == CurrencyPair(Currency.BTC, Currency.USD) }[0]
				   bitstamp_BTCUSD = globalInstruments.filter { it.exchange.impl == Exchange.Impl.BITSTAMP && it.pair == CurrencyPair(Currency.BTC, Currency.USD) }[0]
				   //bitfinex_BTCUSD = globalInstruments.filter { it.exchange.impl == Exchange.Impl.BITFINEX && it.pair == CurrencyPair(Currency.BTC, Currency.USD) }[0]

				   isInitialized = true
			   }

			   if (!GlobalFeatures.immutable.isValid()) {
				   featureEngine.endStreak()
				   return
			   }

			   // features
			   val premium = Math.log(bidAskMid_10kUsd.last / (gdax_BTCUSD.indicators.bidAskMid_10kUsd.last * 0.5 + bitstamp_BTCUSD.indicators.bidAskMid_10kUsd.last * 0.5))

				   val thisVolume = totalTradedInUsd.emaLast
				   val globalVolume = GlobalFeatures.immutable.totalBought.emaLast + GlobalFeatures.immutable.totalSold.emaLast
			   val volumePercentage = if (globalVolume == 0.0) 0.0 else thisVolume / globalVolume

			   val globalNetTraded = GlobalFeatures.immutable.totalNetTraded.emaLast

				   // val bitfinexWeightedReturn = bitfinex_BTCUSD.features.bidAskMid_10kUsd_Return.emaLast * bitfinex_BTCUSD.features.totalTradedInUsd.emaLast
				   val bitstampWeightedReturn = bitstamp_BTCUSD.indicators.bidAskMid_10kUsd_Return.emaLast * bitstamp_BTCUSD.indicators.totalTradedInUsd.emaLast
				   val gdaxWeightedReturn = gdax_BTCUSD.indicators.bidAskMid_10kUsd_Return.emaLast * gdax_BTCUSD.indicators.totalTradedInUsd.emaLast

			   val weightedReturns = if(globalVolume == 0.0) 0.0 else (bitstampWeightedReturn + gdaxWeightedReturn) / globalVolume
			   val myWeightedReturns = if (globalVolume == 0.0) 0.0 else bidAskMid_10kUsd_Return.emaLast * totalTradedInUsd.emaLast /  globalVolume
			   val opsNetTotalToOpsVolume_Ema = (bidAmountAddedIndicator.emaLast - bidAmountRemovedIndicator.emaLast
											 - askAmountAddedIndicator.emaLast + askAmountRemovedIndicator.emaLast) /
											(bidAmountAddedIndicator.emaLast + bidAmountRemovedIndicator.emaLast
											 + askAmountAddedIndicator.emaLast + askAmountRemovedIndicator.emaLast)

				   val bidAskMid = (instrument.bookImmutable.bids[0].price + instrument.bookImmutable.asks[0].price) / 2.0
				   val farBid = instrument.bookImmutable.bids.getFurthestBookEntry(100_000.0)?.price
				   val farAsk = instrument.bookImmutable.asks.getFurthestBookEntry(100_000.0)?.price
				   if (farAsk == null || farBid == null) {
					   featureEngine.endStreak()
					   return
				   }


			   val bookSkew = if (bidAskMid - farBid == 0.0) 0.0 else Math.log((farAsk - bidAskMid) / (bidAskMid - farBid))

			   val averageOrderTimeRatio = if (averageBidsSurvivedTime.emaLast == 0.0) 1.0 else Math.log(averageAsksSurvivedTime.emaLast / averageBidsSurvivedTime.emaLast)
			   val spread = weightedSpread.emaLast
			   val bidAskMidReturn = bidAskMid_10kUsd_Return.last

			   // labels

			   val timeStep = listOf(premium,
									 volumePercentage,
									 globalNetTraded,
									 weightedReturns,
									 myWeightedReturns,
									 opsNetTotalToOpsVolume_Ema,
									 bookSkew,
									 averageOrderTimeRatio,
									 spread,
									 bidAskMidReturn)


			   featureEngine.processTimeStep(timeStep)
	}


	fun writeEmptyStep() {
		featureEngine.processTimeStep(listOf())
	}
}*/
