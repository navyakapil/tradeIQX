
package com.example.tradeiq_x

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

    override suspend fun getQuotes(): List<StockQuote> {
        return symbols.mapNotNull { symbol ->
            getQuote(symbol)
        }
    }

    override suspend fun getQuote(symbol: String): StockQuote? {
        val normalizedSymbol = symbol.trim().uppercase()

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
                currency = "INR",
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

    override suspend fun getHistory(
        symbol: String,
        range: ChartRange
    ): List<PriceCandle> {
        // Historical market data has not been implemented by the backend yet.
        return emptyList()
    }
}


