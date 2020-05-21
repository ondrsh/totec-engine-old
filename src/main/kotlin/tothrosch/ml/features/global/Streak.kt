package tothrosch.ml.features.global

import tothrosch.exchange.Exchange
import tothrosch.util.*

class Streak() {

	/*var startTime = 0L
	var initiated = false
	var streakFactor: Double = 0.0
	lateinit var mex: FeaturePool
	val orderedList: ArrayList<FeaturePool> = arrayListOf()
	var count = -1

	fun isActive(instant: InstantIndicators.BitmexSimple): Boolean {
		if (initiated && instant.makerFactor.sign == streakFactor.sign && (instant.makerFactor + 0.5 * instant.makerFactorDiffEma).abs > 0.2) { // (instant.makerFactor + instant.makerFactorDiffEma).sign == streakFactor.sign) {
			return true
		} else {
			if (initiated) {
				println("streakfactor = $streakFactor")
				println("makerFactor = ${instant.makerFactor}")
				stop()
				println()
				println()
				println()
			}
			return false
		}
	}

	fun stop() {
		orderedList.clear()
		count = -1
		initiated = false
	}

	fun start(currentMex: FeaturePool, currentIndicators: List<FeaturePool>, instant: InstantIndicators.BitmexSimple) {
		mex = currentMex
		orderedList.addAll(currentIndicators.sortedByDescending { Math.abs(it.bidAskMid_10kUsd_Return.emaLast) })
		startTime = now
		initiated = true
		streakFactor = instant.makerFactor
		printStart()
		printLine(instant)
	}

	fun printStart() {
		val line: StringBuilder = StringBuilder()
		line.append("BITMEX")
		for (feat in orderedList) {
			line.append("\t${feat.instrument.exchange.name}")
		}
		println(line.toString())
	}

	fun printLine(instant: InstantIndicators.BitmexSimple) {
		count++
		if (instant.makerFactor.sign == streakFactor.sign && instant.makerFactor.abs > streakFactor.abs) {
			streakFactor = instant.makerFactor
		}
		val line = StringBuilder()
		val gdax = orderedList.filter { it.instrument.exchange.name == Exchange.Name.COINBASE }[0]
		val stamp = orderedList.filter { it.instrument.exchange.name == Exchange.Name.BITSTAMP }[0]
		line.append("Fac: ${instant.makerFactor.format(3, 2)}\t")
		line.append("Diff: ${(instant.makerFactorDiffEma).format(1, 4)}\t")
		line.append("Final: ${instant.makerIndicatorFinal.format(1, 4)}\t")
		// line.append("Ops: ${instant.othersOrderbookSideRatio.format(3,2)}\t")
		line.append("Ops Ema: ${instant.bookSideDiffEma.format(3, 2)}\t")
		line.append("Raw: ${instant.factorRaw.format(3, 2)}\t")
		line.append("Ema: ${instant.factorEma.format(3, 2)}\t")
		line.append("Log Vol Fac: ${instant.mexRelativeVolumeFactor.format(3)}\t")
		line.append("Short: ${instant.short.format(2, 2)}\t")
		line.append("Long: ${instant.long.format(2, 2)}\t")
		line.append("${mex.instrument.bookImmutable.bids.furthestBookEntry(200_000.0)!!.price} / ${mex.instrument.bookImmutable.asks.furthestBookEntry(200_000.0)!!.price}\t")
		line.append("${mex.totalTradedInUsd.emaLast.format(7, 0)} / ${mex.totalNetTradedInUsd.emaLast.format(7, 0)}\t")
		line.append("${mex.bidAskMid_10kUsd_Return.emaLast.format(6)}\t")
		if (gdax != null && stamp != null) {
			if (!gdax.bidAskMid.isEmpty() && !stamp.bidAskMid.isEmpty()) {
				line.append("${((gdax.bidAskMid.last + stamp.bidAskMid.last) / 2.0).format(1)}\t")
			}
		}


		//line.append(createString(mex))

		for (features in orderedList) {
			line.append("\t")
			line.append(createString(features))
		}

		println(line.toString())
	}

	fun createString(featurePool: FeaturePool) =
		featurePool.bidAskMid_10kUsd_Return.emaLast.format(5) + " (${featurePool.totalTradedInUsd.emaLast.format(7, 0)} / ${featurePool.totalNetTradedInUsd.emaLast.format(7, 0)})"
*/
}