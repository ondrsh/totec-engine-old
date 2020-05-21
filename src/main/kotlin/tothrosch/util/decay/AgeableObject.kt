package tothrosch.util.decay

import tothrosch.instrument.Ageable

class AgeableString(val string: String, override val time: Long) : Ageable
class AgeableDouble(val value: Double, override val time: Long) : Ageable