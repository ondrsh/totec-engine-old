package tothrosch.ml.features.strategy

import tothrosch.engine.Global
import tothrosch.engine.mode
import tothrosch.exchange.Exchange
import tothrosch.instrument.Instrument
import tothrosch.instrument.Mode
import tothrosch.ml.features.BitmexFeatureCreator
import tothrosch.ml.features.Feature
import tothrosch.ml.features.Label
import tothrosch.ml.features.Step
import tothrosch.ml.features.write.FeatureWriter
import tothrosch.util.sign

/*
class ReturnVolStrategy(instrument: Instrument) : BitmexFeatureCreator(instrument) {
	
	
	override suspend fun secondFlush() {
		val features = arrayListOf<Feature<Double>>()
		val labels = arrayListOf<Label<Double>>()
		
		if (isInitialized == false) {
			coinbase = Global.instruments.filter { it.exchange.name == Exchange.Name.COINBASE && it.symbol == "BTC-USD" }[0]
			bitfinex = Global.instruments.filter { it.exchange.name == Exchange.Name.BITFINEX && it.symbol == "BTCUSD" }[0]
			binance = Global.instruments.filter { it.exchange.name == Exchange.Name.BINANCE && it.symbol == "BTCUSDT" }[0]
			kraken = Global.instruments.filter { it.exchange.name == Exchange.Name.KRAKEN && it.symbol == "XBT/USD" }[0]
			stepReceiver = if (mode == Mode.FEATUREWRITE) FeatureWriter() else BitmexTradingStrategy(instrument).stepHandler
			isInitialized = true
		}
		
		if (GlobalFeatures.immutable.isValid() == false || coinbase.isActive == false || bitfinex.isActive == false || binance.isActive == false || kraken.isActive == false || instrument.isActive == false) {
			stepReceiver.send(null)
			return
		}
		
		// features
		try {
			features.add(Feature("premium", Math.log(myPool.bidAskMid_10kUsd.lastAverage /
					                                         (coinbase.featurePool.bidAskMid_10kUsd.lastAverage * 0.333 +
							                                         bitfinex.featurePool.bidAskMid_10kUsd.lastAverage * 0.333 +
							                                         binance.featurePool.bidAskMid_10kUsd.lastAverage))))
		} catch (ex: Exception) {
			ex.printStackTrace()
			println()
		}
		
		// TODO use this... also, generally use more ratios to EMAs
		// val thisVolume = totalTradedInUsd.emaLast
		val globalVolume = GlobalFeatures.immutable.totalBought.emaLast + GlobalFeatures.immutable.totalSold.emaLast
		
		
		features.add(Feature("globalNetTraded", GlobalFeatures.immutable.totalNetTraded.emaLast))
		
		
		val coinbaseWeightedReturn = coinbase.featurePool.bidAskMid_10kUsd_Return.emaLast * coinbase.featurePool.totalTradedInUsd.emaLast
		val bitfinexWeightedReturn = bitfinex.featurePool.bidAskMid_10kUsd_Return.emaLast * bitfinex.featurePool.totalTradedInUsd.emaLast
		val binanceWeightedReturn = binance.featurePool.bidAskMid_10kUsd_Return.emaLast * binance.featurePool.totalTradedInUsd.emaLast
		val krakenWeightedReturn = kraken.featurePool.bidAskMid_10kUsd_Return.emaLast * kraken.featurePool.totalTradedInUsd.emaLast
		val totalVolume = coinbase.featurePool.totalTradedInUsd.emaLast +
				bitfinex.featurePool.totalTradedInUsd.emaLast +
				binance.featurePool.totalTradedInUsd.emaLast +
				kraken.featurePool.totalTradedInUsd.emaLast
		
		features.add(Feature("weightedReturns", if (globalVolume == 0.0) 0.0 else (coinbaseWeightedReturn +
				bitfinexWeightedReturn + binanceWeightedReturn + krakenWeightedReturn) / totalVolume))
		
		features.add(Feature("myWeightedEmaReturn", if (globalVolume == 0.0) 0.0 else
			myPool.bidAskMid_10kUsd_Return.emaLast * myPool.totalTradedInUsd.emaLast / totalVolume))
		val bidsAmountNet = myPool.bidAmountAddedIndicator.emaLast - myPool.bidAmountRemovedIndicator.emaLast
		val asksAmountNet = myPool.askAmountAddedIndicator.emaLast - myPool.askAmountRemovedIndicator.emaLast
		val allOpsVol = (myPool.bidAmountAddedIndicator.emaLast + myPool.bidAmountRemovedIndicator.emaLast
				+ myPool.askAmountAddedIndicator.emaLast + myPool.askAmountRemovedIndicator.emaLast)
		features.add(Feature("bidsAmountNetRatio", bidsAmountNet / allOpsVol))
		features.add(Feature("asksAmountNetRatio", asksAmountNet / allOpsVol))
		features.add(Feature("allOpsVolRatio", allOpsVol / myPool.totalTradedInUsd.emaLast))
		val asksSurvivedTime = myPool.averageAsksSurvivedTime.emaLast
		features.add(Feature("asksSurvivedTime", asksSurvivedTime))
		val bidsSurvivedTime = myPool.averageBidsSurvivedTime.emaLast
		features.add(Feature("bidsSurvivedTime", bidsSurvivedTime))
		// TODO do with emas
		val bidAskMid = myPool.bidAskMid.last
		val best10Bids = instrument.bookImmutable.bids.take(10)
		val best10BidsTotalSum = best10Bids.map { it.amount }.sum()
		val best10Asks = instrument.bookImmutable.asks.take(10)
		val best10AsksTotalSum = best10Asks.map { it.amount }.sum()
		features.add(Feature("topTenOrdersAmountRatio", Math.log(best10BidsTotalSum / best10AsksTotalSum)))
		val farBid = best10Bids.map { it.amount * it.price }.sum() / best10BidsTotalSum
		val farAsk = best10Asks.map { it.amount * it.price }.sum() / best10AsksTotalSum
		features.add(Feature("averageBidSize", myPool.averageBidSize.emaLast))
		features.add(Feature("averageAskSize", myPool.averageAskSize.emaLast))
		features.add(Feature("bookSkew", Math.log((farAsk) / (farBid))))
		features.add(Feature("averageSurvivedTimeRatio",
		                     if (myPool.averageBidsSurvivedTime.emaLast == 0.0) 1.0 else Math.log(asksSurvivedTime / bidsSurvivedTime)))
		val net = myPool.totalNetTradedInUsd.lastAverage
		val myNetVolTraded = Math.signum(net) * Math.pow(Math.abs(net), 0.1)
		features.add(Feature("myNetVolAverage", myNetVolTraded))
		features.add(Feature("myRetEma", myPool.bidAskMid_10kUsd_Return.emaLast))
		
		
		// labels
		val ret = myPool.bidAskMid_10kUsd_Return.lastAverage
		val label = if (myNetVolTraded.sign != ret.sign) {
			if (ret == 0.0) {
				myNetVolTraded
			} else {
				0.0
			}
		} else {
			myNetVolTraded * (1 + 100_000 * ret.abs)
		}
		labels.add(Label("myFutureRetAndVolAverage", label))
		
		if (features.isClean() && labels.isClean()) {
		    stepHandler.send(Step(features, labels))
		} else {
			stepHandler.send(null)
		}
	}
	
}*/
