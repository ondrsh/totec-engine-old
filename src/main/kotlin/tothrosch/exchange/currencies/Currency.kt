package tothrosch.exchange.currencies

import tothrosch.engine.Keep

@Keep
enum class Currency(val isFiat: Boolean) {
	ADA(false),
	AGI(false), // SingularityNET
	AID(false),
	AION(false),
	ALGO(false),
	AMP(false),
	ANT(false), // Aragon
	ARDR(false),
	ATOM(false), // Cosmos
	AUD(true),
	AVT(false), // Aventus
	BAT(false), // Basic Attention Token
	BCC(false),
	BCH(false),
	BCHABC(false), // Bitcoin Cash ABC
	BCHSV(false), // Bitcoin SV
	BCN(false),
	BCY(false),
	BEAM(false),
	BELA(false),
	BFT(false), // BnkToTheFuture
	BLK(false),
	BNB(false), // Binance Coin
	BNT(false), // Bancor
	BTC(false),
	BTCD(false),
	BTG(false), // Bitcoin Gold
	BTM(false),
	BTS(false),
	BTT(false), // BitTorrent
	BURST(false),
	CAD(true),
	CHF(true),
	CHZ(false), // Chilliz
	CLAM(false),
	CNY(true),
	COCOS(false), // Cocos-BCX
	COS(false), // Contentos
	CVC(false),
	CZK(true),
	DAI(false),
	DASH(false),
	DATA(false),
	DCR(false),
	DGB(false),
	DNT(false),
	DOCK(false),
	DOGE(false),
	DSH(false),
	DTH(false), // Dether
	EDO(false), // Eidoo
	ELF(false),
	EMC2(false),
	ENJ(false), // Enjin Coin
	EOS(false),
	ETC(false),
	ETH(false),
	ETP(false), // Metaverse
	EUR(true),
	EXP(false),
	FCT(false),
	FET(false), // Fetch
	FLDC(false),
	FLO(false),
	FOAM(false),
	FUEL(false), // Etherparty
	FUN(false), // FunFair
	GAME(false),
	GAS(false),
	GBP(true),
	GNO(false),
	GNT(false),
	GRC(false),
	GRIN(false), // Grin
	GVT(false), // Genesis Vision
	HKD(true),
	HOT(false), // Holo
	HUC(false),
	ICX(false), // Icon
	IDR(true),
	INR(true),
	IOST(false),
	IOTA(false),
	JPY(true),
	KNC(false), // Kyber Network
	KRW(true),
	LBC(false),
	LINK(false),
	LOOM(false),
	LPT(false), // Livepeer
	LRC(false), // Loopring
	LSK(false),
	LTC(false),
	MAID(false),
	MANA(false), // Decentraland
	MATIC(false), // Matic Network
	MDA(false), // Moeda Loyalty Points
	MIT(false),
	MLN(false), // Melon
	MTL(false), // Metal
	MTN(false), // Medicalchain
	NAUT(false),
	NAV(false),
	NEC(false), // nectar tokens
	NEO(false),
	NEOS(false), //NeosCoin
	NKN(false),
	NMC(false),
	NMR(false), // Numeraire
	NOTE(false),
	NXC(false),
	NXT(false),
	ODE(false), // ODEM
	OMG(false),
	OMNI(false),
	ONT(false), // Ontology
	QASH(false),
	QTUM(false),
	PASC(false),
	PHP(true),
	PINK(false),
	POT(false),
	POLY(false), // Polymath
	PPC(false),
	RADS(false),
	RBT(false), // Rimbit
	RCN(false), // Ripio Credit Network
	RDN(false), // Raiden Network Token
	REN(false),
	REQ(false), // Request
	REP(false),
	RIC(false),
	RLC(false), // iExec RLC
	RRT(false),
	RVN(false), // Ravencoin
	SAN(false),
	SBD(false),
	SC(false),
	SGD(true),
	SJCX(false),
	SNGLS(false), // SingularDTV
	SNT(false), // Status
	SPANK(false), // SpankChain
	STEEM(false),
	STORJ(false),
	STRAT(false),
	SYS(false),
	THETA(false),
	TNB(false), // Time New Bank
	TRIO(false), // Tripio
	TRX(false), // Tron
	USD(true),
	VIA(false),
	VRC(false),
	VTC(false),
	WAVES(false),
	WAX(false), // Wax Tokens
	XBC(false),
	XCP(false),
	XEM(false),
	XLM(false), // Stellar
	XMR(false),
	XPM(false),
	XRP(false),
	XTZ(false),
	XVC(false),
	YOYOW(false),
	ZEC(false),
	ZRX(false);
	
}
