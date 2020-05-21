package tothrosch.instrument.candle

import tothrosch.instrument.candle.samplers.SampleType

class ContainerSet(val sampleType: SampleType) : LinkedHashSet<QueueContainer>() {
	
	override fun hashCode() = sampleType.hashCode()
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		if (!super.equals(other)) return false
		
		other as ContainerSet
		
		if (sampleType != other.sampleType) return false
		
		return true
	}
}

