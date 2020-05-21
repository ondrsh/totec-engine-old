package tothrosch.ml.features.queues

import tothrosch.settings.Settings
import kotlin.math.abs
import kotlin.math.ln

class ReturnEmaMaxSizeQueues(emaLength: Int = Settings.emaIndicatorLength,
                             val returns: EmaMaxSizeQueue = EmaMaxSizeQueue(emaLength),
                             val absReturns: EmaMaxSizeQueue = EmaMaxSizeQueue(emaLength)) : EmaMaxSizeQueue(emaLength) {
    
    override fun add(element: Double): Boolean {
        if (super.add(element) == false) return false
        addReturns(element)
        return true
    }
    
    
    private fun addReturns(element: Double) {
        val ret = if (isEmpty()) 0.0  else ln(element / last)
        returns.add(ret)
        absReturns.add(abs(ret))
    }
    
}