package tothrosch.util.time.log

enum class Event {
	BOOK_PROCESSED,
	BOOKOPS_PROCESSED,
	CANDLE_ADDED,
	CREATED,
	REST_EXECUTING_START,
	REST_EXECUTING_END_ERROR,
	REST_EXECUTING_END,
	REST_MAPPING_END,
	REPLAY_BOOKOPS_PROCESSED,
	SEND_TO_CANDLEHUB,
	BACKTESTING_ORDERREQUEST_HANDLED,
	MYTRADES_PROCESSED
	
}