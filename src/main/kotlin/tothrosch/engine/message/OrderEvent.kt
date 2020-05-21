package tothrosch.engine.message

// POST_SINGLE or CLOSEPOSITION are _always_ takers
// POST and AMEND are _always_ makers.
enum class OrderEvent {
	POST,
	POST_SINGLE,
	AMEND,
	CANCEL,
	CANCELALL,
	CLOSEPOSITION,
}