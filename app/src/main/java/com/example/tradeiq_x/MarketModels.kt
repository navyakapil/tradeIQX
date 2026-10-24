package com.example.tradeiq_x
data class StockQuote(
    val symbol: String,
    val companyName: String,
    val price: Double,
    val previousClose: Double,
    val currency: String = "INR",
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
