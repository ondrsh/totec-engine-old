package tothrosch.exchange

import tothrosch.exchange.currencies.CurrencyPair


class SupportedPairs(pairs: Set<CurrencyPair> = setOf()) : HashSet<CurrencyPair>(pairs)