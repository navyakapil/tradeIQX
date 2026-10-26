package com.example.tradeiq_x

object MarketConstants {
    const val STALE_THRESHOLD_MS: Long = 60_000L // 60 seconds
    const val NETWORK_CONNECT_TIMEOUT_MS: Int = 10_000
    const val NETWORK_READ_TIMEOUT_MS: Int = 15_000
    const val DEFAULT_CURRENCY: String = "INR"
    const val DEFAULT_EXCHANGE: String = "NSE"

    // Time intervals in milliseconds
    const val MS_PER_MINUTE: Long = 60_000L
    const val MS_PER_HOUR: Long = 60L * MS_PER_MINUTE
    const val MS_PER_DAY: Long = 24L * MS_PER_HOUR
    const val MS_PER_MONTH: Long = 30L * MS_PER_DAY

    // Candle counts for simulated chart ranges
    const val CANDLE_COUNT_1D: Int = 24
    const val CANDLE_COUNT_1W: Int = 7
    const val CANDLE_COUNT_1M: Int = 30
    const val CANDLE_COUNT_1Y: Int = 12
}

/**
 * Normalizes and validates a stock ticker symbol.
 * Strips whitespace, converts to uppercase, removes 'NSE:' / 'NSE_' prefixes
 * and '.NS' suffixes, and verifies the symbol is 1-20 alphanumeric characters.
 */
fun normalizeMarketSymbol(rawSymbol: String): String {
    var symbol = rawSymbol.trim().uppercase()
    if (symbol.startsWith("NSE:") || symbol.startsWith("NSE_")) {
        symbol = symbol.substring(4).trim()
    }
    if (symbol.endsWith(".NS")) {
        symbol = symbol.removeSuffix(".NS").trim()
    }
    require(symbol.isNotEmpty() && symbol.matches(Regex("^[A-Z0-9&-]{1,20}$"))) {
        "Invalid stock symbol: $rawSymbol"
    }
    return symbol
}

data class StockQuote(
    val symbol: String,
    val companyName: String,
    val price: Double,
    val previousClose: Double,
    val currency: String = MarketConstants.DEFAULT_CURRENCY,
    val lastUpdated: String? = null
) {
    val change: Double
        get() = price - previousClose

    val changePercent: Double
        get() = if (previousClose > 0.0) {
            (change / previousClose) * 100.0
        } else {
            0.0
        }
}

data class PriceCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 0L
)

enum class ChartRange(val label: String) {
    ONE_DAY("1D"),
    ONE_WEEK("1W"),
    ONE_MONTH("1M"),
    ONE_YEAR("1Y")
}

data class MarketDataResult<T>(
    val data: T? = null,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = data != null && error == null
}
