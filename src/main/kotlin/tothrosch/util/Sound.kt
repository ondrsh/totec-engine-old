package tothrosch.util

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import tothrosch.settings.Settings
import java.io.File
import javax.sound.sampled.*

object Sound {

	suspend fun playSound() {
		soundplayer.send(true)
	}

	private suspend fun playSoundInternal() {
		try {
			val yourFile: File
			val stream: AudioInputStream
			val format: AudioFormat
			val info: DataLine.Info
			val clip: Clip
			
			stream = AudioSystem.getAudioInputStream(File("/home/ndrsh/Downloads/beep.wav"))
			format = stream.format
			info = DataLine.Info(Clip::class.java, format)
			clip = AudioSystem.getLine(info) as Clip
			clip.open(stream)
			clip.start()
			delay(2000)
		} catch (ex: Exception) {
			System.out.println("Error with playing sound.");
			ex.printStackTrace();
		}
	}


	private val soundplayer = GlobalScope.actor<Boolean>(Settings.appContext, 10) {
		for (msg in channel) {
			println("beep")
			playSoundInternal()
		}
	}
}


