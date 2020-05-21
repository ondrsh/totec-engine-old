package tothrosch.util

import kotlinx.coroutines.CoroutineScope
import tothrosch.settings.Settings
import kotlin.coroutines.CoroutineContext

interface AppScope: CoroutineScope {
	override val coroutineContext: CoroutineContext
		get() = Settings.appContext
}