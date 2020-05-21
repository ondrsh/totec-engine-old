package tothrosch.instrument.handlers

import tothrosch.engine.message.Message
import tothrosch.instrument.Instrument
import tothrosch.instrument.Update

class InstrumentUpdateHandler(val instrument: Instrument) : MessageHandler<Update, Update>(10) {
	
	
	override fun handleMessage(msg: Message<Update>): Message<Update> {
		instrument.time.update(msg.timestamp)
		instrument.processInstrumentUpdate(msg.content)
		return msg
	}
	
	
	/*class Spot(instrument: Instrument.Spot): InstrumentUpdateHandler(instrument) {
		
		override fun update(updateMessage: Message<Update>) {
		
		}
	}
	
	
	sealed class Bitmex(instrument: Instrument.Bitmex): InstrumentUpdateHandler(instrument) {
		
		class Swap(instrument: Instrument.Bitmex.Swap): InstrumentUpdateHandler.Bitmex(instrument) {
			override fun update(updateMessage: Message<Update>) {
			
			}
		}
		
		class Future(instrument: Instrument.Bitmex.Future): InstrumentUpdateHandler.Bitmex(instrument) {
			override fun update(updateMessage: Message<Update>) {
			
			}
		}
	}*/
	
}
