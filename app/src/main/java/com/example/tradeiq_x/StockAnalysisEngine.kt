
package com.example.tradeiq_x

import java.util.Locale
import kotlin.math.abs

data class AnalysisResult(
    val signal: String,
    val risk: String,
    val explanation: String
)

object StockAnalysisEngine {

    // Simulated historical prices for educational use.
    // 15 prices are provided for calculating RSI(14).
    private val priceHistory = mapOf(
        "RELIANCE" to listOf(
            1390.0, 1400.0, 1395.0, 1410.0, 1405.0,
            1420.0, 1415.0, 1430.0, 1425.0, 1435.0,
            1430.0, 1440.0, 1435.0, 1445.0, 1450.25
        ),
        "TCS" to listOf(
            3090.0, 3100.0, 3095.0, 3120.0, 3110.0,
            3130.0, 3125.0, 3150.0, 3140.0, 3160.0,
            3155.0, 3180.0, 3170.0, 3190.0, 3200.0
        ),
        "INFY" to listOf(
            1580.0, 1570.0, 1575.0, 1560.0, 1565.0,
            1550.0, 1555.0, 1540.0, 1545.0, 1530.0,
            1535.0, 1520.0, 1525.0, 1510.0, 1500.0
        ),
        "HDFCBANK" to listOf(
            1690.0, 1700.0, 1695.0, 1710.0, 1705.0,
            1690.0, 1700.0, 1715.0, 1705.0, 1695.0,
            1700.0, 1710.0, 1695.0, 1705.0, 1700.0
        )
    )

    private fun movingAverage(
        prices: List<Double>,
        period: Int
    ): Double? {
        if (period <= 0 || prices.size < period) return null
        return prices.takeLast(period).average()
    }

    private fun calculateRSI(
        prices: List<Double>,
        period: Int = 14
    ): Double? {
        if (period <= 0 || prices.size <= period) return null

        val changes = prices.takeLast(period + 1)
            .zipWithNext { previous, current ->
                current - previous
            }

        val averageGain = changes.sumOf {
            if (it > 0) it else 0.0
        } / period

        val averageLoss = changes.sumOf {
            if (it < 0) -it else 0.0
        } / period

        if (averageLoss == 0.0) {
            return if (averageGain == 0.0) 50.0 else 100.0
        }

        val relativeStrength = averageGain / averageLoss

        return 100.0 - (100.0 / (1.0 + relativeStrength))
    }

    fun analyze(symbol: String): AnalysisResult {
        val history = priceHistory[symbol.uppercase()]
            ?: return AnalysisResult(
                signal = "HOLD",
                risk = "Unknown",
                explanation = "Historical data is unavailable."
            )

        val firstPrice = history.first()
        val lastPrice = history.last()

        val changePercent =
            ((lastPrice - firstPrice) / firstPrice) * 100.0

        val ma5 = movingAverage(history, 5)
        val rsi14 = calculateRSI(history)

        if (ma5 == null || rsi14 == null) {
            return AnalysisResult(
                signal = "HOLD",
                risk = "Unknown",
                explanation = "Not enough historical data for analysis."
            )
        }

        // Educational rule-based signals, not AI predictions.
        val signal = when {
            lastPrice > ma5 && rsi14 < 70.0 -> "BUY"
            lastPrice < ma5 && rsi14 > 30.0 -> "SELL"
            else -> "HOLD"
        }

        // Demo volatility indicator based on sample price changes.
        val risk = when {
            abs(changePercent) > 4.0 -> "High"
            abs(changePercent) > 2.0 -> "Medium"
            else -> "Low"
        }

        val explanation = buildString {
            append(
                "Simulated price change: ${
                    String.format(Locale.US, "%.2f", changePercent)
                }%. "
            )
            append(
                "5-period moving average: ${
                    String.format(Locale.US, "%.2f", ma5)
                }. "
            )
            append(
                "RSI (14): ${
                    String.format(Locale.US, "%.2f", rsi14)
                }. "
            )
            append("Signal calculated using educational technical rules.")
        }

        return AnalysisResult(
            signal = signal,
            risk = risk,
            explanation = explanation
        )
    }
}
