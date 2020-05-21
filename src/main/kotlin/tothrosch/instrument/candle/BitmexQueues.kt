package tothrosch.instrument.candle

import tothrosch.instrument.Instrument
import tothrosch.util.MaxSizeArrayDeque

open class BitmexQueues(instrument: Instrument.Bitmex,
                        val openInterest: MaxSizeArrayDeque<Double> = MaxSizeArrayDeque(),
                        val vwap: MaxSizeArrayDeque<Double> = MaxSizeArrayDeque()) : QueueContainer(instrument)

class BitmexSwapQueues(instrument: Instrument.Bitmex.Swap,
                       val fundingRate: MaxSizeArrayDeque<Double> = MaxSizeArrayDeque(),
                       val indicativeFundingRate: MaxSizeArrayDeque<Double>) : BitmexQueues(instrument)

