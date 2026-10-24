
package com.example.tradeiq_x

data class AnalysisResult(
    val signal: String,
    val risk: String,
    val explanation: String
)

object StockAnalysisEngine {

    // Sample historical prices for educational testing.
    private val priceHistory = mapOf(
        "RELIANCE" to listOf(1400.0, 1420.0, 1435.0, 1450.25),
        "TCS" to listOf(3100.0, 3130.0, 3170.0, 3200.0),
        "INFY" to listOf(1570.0, 1550.0, 1520.0, 1500.0),
        "HDFCBANK" to listOf(1690.0, 1710.0, 1680.0, 1700.0)
    )

    fun analyze(symbol: String): AnalysisResult {
        val history = priceHistory[symbol.uppercase()]
            ?: return AnalysisResult(
                "HOLD",
                "Unknown",
                "Historical data is unavailable."
            )

        val firstPrice = history.first()
        val lastPrice = history.last()

        val changePercent =
            ((lastPrice - firstPrice) / firstPrice) * 100

        val signal = when {
            changePercent > 2 -> "BUY"
            changePercent < -2 -> "SELL"
            else -> "HOLD"
        }

        val risk = when {
            kotlin.math.abs(changePercent) > 4 -> "High"
            kotlin.math.abs(changePercent) > 2 -> "Medium"
            else -> "Low"
        }

        val explanation =
            "The simulated price changed by " +
                    "%.2f".format(changePercent) +
                    "% over the sample period. " +
                    "This is a basic trend-based signal."

        return AnalysisResult(signal, risk, explanation)
    }
}


