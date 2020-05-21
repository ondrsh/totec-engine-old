package tothrosch.ml.features

import tothrosch.instrument.candle.samplers.SampleType
import tothrosch.ml.features.strategy.StrategyName

class Step<T, R>(val features: List<Feature<T>>,
                 val labels: List<Label<R>>,
                 val timeStep: Long)
