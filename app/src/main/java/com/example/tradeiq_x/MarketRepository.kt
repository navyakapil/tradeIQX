package com.example.tradeiq_x

/**
 * Repository interface providing market quotes and historical candle data.
 * Used by market screens and portfolio valuation across the TradeIQX application.
 */
interface MarketRepository {
    /**
     * Fetches current stock quotes for all tracked default watchlist symbols.
     *
     * @return List of [StockQuote] objects available from the provider.
     */
    suspend fun getQuotes(): List<StockQuote>

    /**
     * Fetches the latest quote for a specific stock ticker symbol.
     *
     * @param symbol The ticker symbol (e.g. "RELIANCE", "TCS").
     * @return The [StockQuote] if found and valid, or `null` if unavailable or on failure.
     */
    suspend fun getQuote(symbol: String): StockQuote?

    /**
     * Fetches historical OHLCV candle data for the specified symbol and time range.
     *
     * @param symbol The stock ticker symbol.
     * @param range The time window requested ([ChartRange.ONE_DAY], [ChartRange.ONE_WEEK], etc.).
     * @return Ordered list of [PriceCandle] entries from oldest to newest.
     */
    suspend fun getHistory(
        symbol: String,
        range: ChartRange
    ): List<PriceCandle>
}
