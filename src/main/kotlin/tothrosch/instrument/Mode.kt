package tothrosch.instrument

import tothrosch.engine.Keep

@Keep
enum class Mode {
	BACKTEST_LIVE {
		override val isLive = true
		override val needsSamplers = true
		override val isBacktesting = true
	},
	BACKTEST {
		override val isLive = false
		override val needsSamplers = true
		override val isBacktesting = true
	},
	TRADE {
		override val isLive = true
		override val needsSamplers = true
		override val isBacktesting = false
	},
	LISTEN {
		override val isLive = true
		override val needsSamplers = false
		override val isBacktesting = false
	},
	WRITE {
		override val isLive = true
		override val needsSamplers = false
		override val isBacktesting = false
	},
	FEATUREWRITE {
		override val isLive = false
		override val needsSamplers = true
		override val isBacktesting = false
	},
	READ {
		override val isLive = false
		override val needsSamplers = false
		override val isBacktesting = false
	},
	START {
		override val isLive = false
		override val needsSamplers = false
		override val isBacktesting = false
	};
	
	
	abstract val isLive: Boolean
	abstract val needsSamplers: Boolean
	abstract val isBacktesting: Boolean
	
}