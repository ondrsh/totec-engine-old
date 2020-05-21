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

/*
class ReturnStrategy(instrument: Instrument) : BitmexFeatureCreator(instrument) {
	
	override suspend fun secondFlush() {
		val features = arrayListOf<Feature<Double>>()
		val labels = arrayListOf<Label<Double>>()
		
		if (isInitialized == false) {
			coinbase = Global.instruments.filter { it.exchange.name == Exchange.Name.COINBASE && it.symbol == "BTC-USD" }[0]
			bitfinex = Global.instruments.filter { it.exchange.name == Exchange.Name.BITFINEX && it.symbol == "BTCUSD" }[0]
			binance = Global.instruments.filter { it.exchange.name == Exchange.Name.BINANCE && it.symbol == "BTCUSDT" }[0]
			kraken = Global.instruments.filter { it.exchange.name == Exchange.Name.KRAKEN && it.symbol == "XBT/USD" }[0]
			stepReceiver = if (mode == Mode.FEATUREWRITE) FeatureWriter() else Backtest()
			isInitialized = true
		}
		
		if (GlobalFeatures.immutable.isValid() == false) {
			stepReceiver.invalidStep()
			return
		}
		
		// TODO use this... also, generally use more ratios to EMAs
		val globalVolume = GlobalFeatures.immutable.totalBought.emaLast + GlobalFeatures.immutable.totalSold.emaLast
		val globalBoughtRatio = Math.log(GlobalFeatures.immutable.totalBought.emaLast/GlobalFeatures.immutable.totalSold.emaLast)
		features.add(Feature("globalBoughtRatio", globalBoughtRatio))
		features.add(Feature("globalNetTraded", GlobalFeatures.immutable.totalNetTraded.emaLast))
		features.add(Feature("globalNetTradedNowToTotalEma", GlobalFeatures.immutable.totalNetTraded.emaLast / globalVolume))
		
		features.add(Feature("coinbaseReturn", coinbase.featurePool.bidAskMid_10kUsd_Return.emaLast))
		features.add(Feature("bitfinexReturn", bitfinex.featurePool.bidAskMid_10kUsd_Return.emaLast))
		features.add(Feature("binanceReturn", binance.featurePool.bidAskMid_10kUsd_Return.emaLast))
		features.add(Feature("krakenReturn", kraken.featurePool.bidAskMid_10kUsd_Return.emaLast))
		
		features.add(Feature("coinbaseNetVol", coinbase.featurePool.totalNetTradedInUsd.emaLast))
		features.add(Feature("bitfinexNetVol", bitfinex.featurePool.totalNetTradedInUsd.emaLast))
		features.add(Feature("binanceNetVol", binance.featurePool.totalNetTradedInUsd.emaLast))
		features.add(Feature("krakenNetVol", kraken.featurePool.totalNetTradedInUsd.emaLast))
		
		*/
/*	val coinbaseWeightedReturn = coinbase.featurePool.bidAskMid_10kUsd_Return.emaLast * coinbase.featurePool.totalTradedInUsd.emaLast
			val bitfinexWeightedReturn = bitfinex.featurePool.bidAskMid_10kUsd_Return.emaLast * bitfinex.featurePool.totalTradedInUsd.emaLast
			val binanceWeightedReturn = binance.featurePool.bidAskMid_10kUsd_Return.emaLast * binance.featurePool.totalTradedInUsd.emaLast
			val krakenWeightedReturn = kraken.featurePool.bidAskMid_10kUsd_Return.emaLast * kraken.featurePool.totalTradedInUsd.emaLast
			val totalVolume = coinbase.featurePool.totalTradedInUsd.emaLast +
					bitfinex.featurePool.totalTradedInUsd.emaLast +
					binance.featurePool.totalTradedInUsd.emaLast +
					kraken.featurePool.totalTradedInUsd.emaLast*//*

		
		*/
/*
				features.add(Feature("weightedReturns", if (globalVolume == 0.0) 0.0 else (coinbaseWeightedReturn +
						bitfinexWeightedReturn + binanceWeightedReturn + krakenWeightedReturn) / totalVolume))*//*

		
		val bidsAmountAdded = myPool.bidAmountAddedIndicator.emaLast
		val bidsAmountRemoved = myPool.bidAmountRemovedIndicator.emaLast
		val asksAmountAdded = myPool.askAmountAddedIndicator.emaLast
		val asksAmountRemoved = myPool.askAmountRemovedIndicator.emaLast
		val allOpsVol = (bidsAmountAdded + bidsAmountRemoved
				+ asksAmountAdded + asksAmountRemoved)
		features.add(Feature("bidsAmountAddedRatio", bidsAmountAdded / allOpsVol))
		features.add(Feature("bidsAmountRemovedRatio", bidsAmountRemoved / allOpsVol))
		features.add(Feature("asksAmountAddedRatio", asksAmountAdded / allOpsVol))
		features.add(Feature("asksAmountRemovedRatio", asksAmountRemoved / allOpsVol))
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
		features.add(Feature("topTenOrdersAmountRatio", Math.log(best10BidsTotalSum/best10AsksTotalSum)))
		val farBid = best10Bids.map { it.amount * it.price }.sum() / best10BidsTotalSum
		val farAsk = best10Asks.map { it.amount * it.price }.sum() / best10AsksTotalSum
		
		val tradesCountToOpsCount = 1.0 * myPool.trades.maxSizeQueue.toList().flatten().size / myPool.ops.maxSizeQueue.toList().flatten().size
		features.add(Feature("tradeCountToOpsCount", tradesCountToOpsCount))
		
		features.add(Feature("averageBidSize", myPool.averageBidSize.emaLast))
		features.add(Feature("averageBidSizeVar", myPool.averageBidSize.emaMaxSizeList.rawDeque.innerList.variance()))
		
		features.add(Feature("averageAskSize", myPool.averageAskSize.emaLast))
		features.add(Feature("averageAskSizeVar", myPool.averageAskSize.emaMaxSizeList.rawDeque.innerList.variance()))
		
		features.add(Feature("bookSkew", Math.log((farAsk - bidAskMid) / (bidAskMid - farBid))))
		features.add(Feature("averageSurvivedTimeRatio", if (myPool.averageBidsSurvivedTime.emaLast == 0.0) 1.0 else Math.log(asksSurvivedTime / bidsSurvivedTime)))
		features.add(Feature("myVolToGlobalVol", myPool.totalTradedInUsd.emaLast / globalVolume))
		val net = myPool.totalNetTradedInUsd.emaLast
		features.add(Feature("myNetVolAverage", net))
		features.add(Feature("myNowVolToEmaVol", myPool.totalTradedInUsd.emaMaxSizeList.rawDeque.lastStride / myPool.totalTradedInUsd.emaMaxSizeList.emaLast ))
		features.add(Feature("myRetEma", myPool.bidAskMid_10kUsd_Return.emaLast))
		features.add(Feature("myVar", myPool.bidAskMid_10kUsd.emaMaxSizeList.rawDeque.innerList.variance() / myPool.bidAskMid_10kUsd.emaLast))
		
		// labels
		val ret = myPool.scaledReturn.lastAverage
		*/
/*	val label = if (myNetVolTraded.sign != ret.sign) {
				if (ret == 0.0) {
					myNetVolTraded
				} else {
					0.0
				}
			} else {
				myNetVolTraded * (1 + 100_000 * ret.abs)
			}*//*

		labels.add(Label("myFutureRet", ret))
		
		if (features.isClean() && labels.isClean()) {
			stepReceiver.receiveStep(Step(features, labels))
		} else {
			stepReceiver.invalidStep()
		}
	}
	
}*/
