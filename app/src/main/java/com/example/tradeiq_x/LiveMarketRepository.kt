package com.example.tradeiq_x

import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Live market repository implementation backed by the TradeIQX FastAPI market service
 * and upstream Groww market data API.
 */
class LiveMarketRepository : MarketRepository {

    private val symbols = listOf(
        "RELIANCE",
        "TCS",
        "INFY",
        "HDFCBANK",
        "ICICIBANK",
        "ITC",
        "SBIN",
        "WIPRO"
    )

    private val companyNames = mapOf(
        "RELIANCE" to "Reliance Industries",
        "TCS" to "Tata Consultancy Services",
        "INFY" to "Infosys Limited",
        "HDFCBANK" to "HDFC Bank",
        "ICICIBANK" to "ICICI Bank",
        "ITC" to "ITC Limited",
        "SBIN" to "State Bank of India",
        "WIPRO" to "Wipro Limited"
    )

    /**
     * Executes an operation with bounded retry and exponential backoff for transient failures.
     * Rejects retrying for non-transient 4xx HTTP responses, rate limit status codes (429),
     * or symbol validation exceptions.
     */
    private suspend fun <T> retryTransient(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 250L,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (exception: Exception) {
                lastException = exception
                val message = exception.message.orEmpty()

                // Check for non-retryable conditions: 4xx errors, rate limits, or validation failures
                val isNonRetryable = exception is IllegalArgumentException ||
                        message.contains("400") ||
                        message.contains("401") ||
                        message.contains("403") ||
                        message.contains("404") ||
                        message.contains("422") ||
                        message.contains("429") ||
                        message.contains("rate limit", ignoreCase = true) ||
                        message.contains("Invalid stock symbol", ignoreCase = true)

                if (isNonRetryable || attempt == maxAttempts) {
                    throw exception
                }

                delay(currentDelay)
                currentDelay *= 2
            }
        }
        throw lastException ?: IOException("Request failed after $maxAttempts attempts")
    }

    /**
     * Fetches live quotes for all default watchlist symbols concurrently.
     * Skips symbols that fail to return valid quotes.
     */
    override suspend fun getQuotes(): List<StockQuote> {
        return symbols.mapNotNull { symbol ->
            getQuote(symbol)
        }
    }

    /**
     * Fetches the live quote for a given symbol from the backend API.
     * Retries transient network failures with exponential backoff up to 3 times.
     *
     * @param symbol Ticker symbol to query (e.g. "RELIANCE", "TCS.NS", "NSE_INFY").
     * @return [StockQuote] with latest price and timestamp, or null on error.
     */
    override suspend fun getQuote(symbol: String): StockQuote? {
        val normalizedSymbol = try {
            normalizeMarketSymbol(symbol)
        } catch (_: IllegalArgumentException) {
            return null
        }

        return try {
            val response = retryTransient {
                BackendApi.getLiveQuote(normalizedSymbol)
            }
            val price = response.optDouble("price", 0.0)

            if (!price.isFinite() || price <= 0.0) {
                return null
            }

            val previousClose = if (response.has("previous_close") && !response.isNull("previous_close")) {
                response.optDouble("previous_close").takeIf { it.isFinite() && it > 0.0 }
            } else null

            val change = if (response.has("change") && !response.isNull("change")) {
                response.optDouble("change").takeIf { it.isFinite() }
            } else previousClose?.let { price - it }

            val changePercent = if (response.has("change_percent") && !response.isNull("change_percent")) {
                response.optDouble("change_percent").takeIf { it.isFinite() }
            } else previousClose?.let { if (it > 0.0) ((price - it) / it) * 100.0 else null }

            StockQuote(
                symbol = normalizedSymbol,
                companyName = companyNames[normalizedSymbol]
                    ?: normalizedSymbol,
                price = price,
                previousClose = previousClose,
                change = change,
                changePercent = changePercent,
                currency = MarketConstants.DEFAULT_CURRENCY,
                lastUpdated = response.optString("fetched_at")
                    .takeIf { it.isNotBlank() },
                status = response.optString("status", "LIVE")
            )
        } catch (exception: Exception) {
            val message = exception.message.orEmpty()
            val inferredStatus = when {
                message.contains("provider_unauthorised") || message.contains("403") -> "UNAUTHORISED"
                message.contains("Connection refused") || message.contains("failed to connect") || message.contains("unavailable") -> "OFFLINE"
                else -> "OFFLINE"
            }
            android.util.Log.e(
                "LiveMarketRepository",
                "Failed to fetch quote for $normalizedSymbol ($inferredStatus): ${exception.message}"
            )
            val fallback = MockMarketRepository().getQuote(normalizedSymbol)
            fallback?.copy(status = inferredStatus)
        }
    }

    var isLastHistoryDemo: Boolean = false
        private set

    /**
     * Fetches historical candle data for chart rendering from the backend service.
     * Falls back to deterministic mock candles WITH a clear demo indicator if the live history
     * endpoint is unauthorized or offline. Never presents demo candles as real.
     */
    override suspend fun getHistory(
        symbol: String,
        range: ChartRange
    ): List<PriceCandle> {
        val normalizedSymbol = try {
            normalizeMarketSymbol(symbol)
        } catch (_: IllegalArgumentException) {
            isLastHistoryDemo = true
            return emptyList()
        }

        return try {
            val response = retryTransient {
                BackendApi.getHistory(normalizedSymbol, range.label)
            }
            val candlesArray = response.optJSONArray("candles")
            if (candlesArray != null && candlesArray.length() > 0) {
                val parsed = mutableListOf<PriceCandle>()
                for (i in 0 until candlesArray.length()) {
                    val c = candlesArray.getJSONObject(i)
                    parsed.add(
                        PriceCandle(
                            timestamp = c.optLong("timestamp"),
                            open = c.optDouble("open"),
                            high = c.optDouble("high"),
                            low = c.optDouble("low"),
                            close = c.optDouble("close"),
                            volume = c.optLong("volume", 0L)
                        )
                    )
                }
                isLastHistoryDemo = false
                parsed
            } else {
                isLastHistoryDemo = true
                MockMarketRepository().getHistory(normalizedSymbol, range)
            }
        } catch (exception: Exception) {
            android.util.Log.w(
                "LiveMarketRepository",
                "Backend history unavailable for $normalizedSymbol (${exception.message}); returning demo candles with demo tag"
            )
            isLastHistoryDemo = true
            MockMarketRepository().getHistory(normalizedSymbol, range)
        }
    }
}
