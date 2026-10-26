
package com.example.tradeiq_x

class MockMarketRepository : MarketRepository {

    private val quotes = listOf(
        StockQuote(
            "RELIANCE", "Reliance Industries",
            1450.25, 1431.35
        ),
        StockQuote(
            "TCS", "Tata Consultancy Services",
            3920.50, 3887.46
        ),
        StockQuote(
            "INFY", "Infosys Limited",
            1620.30, 1610.64
        ),
        StockQuote(
            "HDFCBANK", "HDFC Bank",
            1780.90, 1788.40
        ),
        StockQuote(
            "ICICIBANK", "ICICI Bank",
            1245.60, 1231.44
        ),
        StockQuote(
            "ITC", "ITC Limited",
            420.80, 421.85
        ),
        StockQuote(
            "SBIN", "State Bank of India",
            725.60, 719.13
        ),
        StockQuote(
            "WIPRO", "Wipro Limited",
            290.40, 291.42
        )
    )

    override suspend fun getQuotes(): List<StockQuote> {
        return quotes
    }

    override suspend fun getQuote(
        symbol: String
    ): StockQuote? {
        return quotes.find {
            it.symbol.equals(symbol, ignoreCase = true)
        }
    }

    override suspend fun getHistory(
        symbol: String,
        range: ChartRange
    ): List<PriceCandle> {
        val quote = getQuote(symbol) ?: return emptyList()
        val count = when (range) {
            ChartRange.ONE_DAY -> 24
            ChartRange.ONE_WEEK -> 7
            ChartRange.ONE_MONTH -> 30
            ChartRange.ONE_YEAR -> 12
        }

        val intervalMs = when (range) {
            ChartRange.ONE_DAY -> 60L * 60 * 1000
            ChartRange.ONE_WEEK -> 24L * 60 * 60 * 1000
            ChartRange.ONE_MONTH -> 24L * 60 * 60 * 1000
            ChartRange.ONE_YEAR -> 30L * 24 * 60 * 60 * 1000
        }

        val now = System.currentTimeMillis()
        val basePrice = quote.price

        return (0 until count).map { index ->
            val variation =
                kotlin.math.sin(index.toDouble() * 0.7) * 0.015

            val open = basePrice * (1 + variation)
            val close = basePrice *
                    (1 + variation +
                            kotlin.math.cos(index.toDouble()) * 0.004)

            PriceCandle(
                timestamp = now -
                        (count - 1 - index) * intervalMs,
                open = open,
                high = maxOf(open, close) * 1.003,
                low = minOf(open, close) * 0.997,
                close = close
            )
        }
    }
}


