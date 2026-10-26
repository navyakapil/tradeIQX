
package com.example.tradeiq_x

interface MarketRepository {
    suspend fun getQuotes(): List<StockQuote>

    suspend fun getQuote(symbol: String): StockQuote?

    suspend fun getHistory(
        symbol: String,
        range: ChartRange
    ): List<PriceCandle>
}


