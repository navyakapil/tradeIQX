package com.example.tradeiq_x

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
            val response = BackendApi.getLiveQuote(normalizedSymbol)
            val price = response.optDouble("price", 0.0)

            if (!price.isFinite() || price <= 0.0) {
                return null
            }

            StockQuote(
                symbol = normalizedSymbol,
                companyName = companyNames[normalizedSymbol]
                    ?: normalizedSymbol,
                price = price,
                previousClose = 0.0,
                currency = MarketConstants.DEFAULT_CURRENCY,
                lastUpdated = response.optString("fetched_at")
                    .takeIf { it.isNotBlank() }
            )
        } catch (exception: Exception) {
            android.util.Log.e(
                "LiveMarketRepository",
                "Failed to fetch quote for $normalizedSymbol",
                exception
            )
            null
        }
    }

    /**
     * Fetches historical candle data for chart rendering.
     * Currently returns an empty list pending backend historical data availability.
     */
    override suspend fun getHistory(
        symbol: String,
        range: ChartRange
    ): List<PriceCandle> {
        // Historical market data has not been implemented by the backend yet.
        return emptyList()
    }
}
