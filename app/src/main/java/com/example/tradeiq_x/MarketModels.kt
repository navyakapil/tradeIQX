package com.example.tradeiq_x

import androidx.compose.ui.graphics.Color

enum class MarketDataStatus(
    val displayName: String,
    val badgeColor: Color,
    val textColor: Color
) {
    LIVE("LIVE", Color(0xFFDCFCE7), Color(0xFF166534)),
    DELAYED("DELAYED", Color(0xFFFEF3C7), Color(0xFF92400E)),
    DEMO("DEMO", Color(0xFFE0E7FF), Color(0xFF3730A3)),
    UNAUTHORISED("PROVIDER NOT AUTHORISED", Color(0xFFFFEDD5), Color(0xFF9A3412)),
    OFFLINE("OFFLINE", Color(0xFFFEE2E2), Color(0xFF991B1B));

    fun noticeText(): String = when (this) {
        LIVE -> "Live Market Data · Real-time NSE"
        DELAYED -> "Market Data Delayed · Last known prices"
        DEMO -> "Demo Mode · Simulated Market Data"
        UNAUTHORISED -> "Provider Not Authorised · Check Groww API permissions"
        OFFLINE -> "Market Service Offline · Connect to backend"
    }

    companion object {
        fun fromString(value: String?): MarketDataStatus {
            return when (value?.uppercase()?.trim()) {
                "LIVE" -> LIVE
                "DELAYED", "STALE" -> DELAYED
                "DEMO" -> DEMO
                "UNAUTHORISED", "UNAUTHORIZED", "PROVIDER_UNAUTHORISED" -> UNAUTHORISED
                "OFFLINE" -> OFFLINE
                else -> DEMO
            }
        }
    }
}


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

    // User-facing market data status notices
    const val NOTICE_DEMO_DATA: String = "Demo Mode · Simulated Market Data"
    const val NOTICE_STALE_DATA: String = "Market Data Delayed · Last known prices"
    const val NOTICE_LIVE_DATA: String = "Live Market Data · Real-time NSE"
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
    val previousClose: Double? = null,
    val change: Double? = null,
    val changePercent: Double? = null,
    val currency: String = MarketConstants.DEFAULT_CURRENCY,
    val lastUpdated: String? = null,
    val status: String = "LIVE"
) {
    constructor(
        symbol: String,
        companyName: String,
        price: Double,
        previousClose: Double?,
        currency: String = MarketConstants.DEFAULT_CURRENCY,
        lastUpdated: String? = null,
        status: String = "LIVE"
    ) : this(
        symbol = symbol,
        companyName = companyName,
        price = price,
        previousClose = previousClose,
        change = previousClose?.let { price - it },
        changePercent = previousClose?.let { if (it > 0.0) ((price - it) / it) * 100.0 else null },
        currency = currency,
        lastUpdated = lastUpdated,
        status = status
    )
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
