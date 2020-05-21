package tothrosch.instrument

import tothrosch.engine.Keep

@Keep
class Instruments(instruments: Set<Instrument> = setOf()) : Set<Instrument> by instruments