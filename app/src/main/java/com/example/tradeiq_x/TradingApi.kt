package com.example.tradeiq_x

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class TradeOrderResult(
    val message: String,
    val orderId: Long?,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val cashBalance: Double
)

data class HoldingItem(
    val symbol: String,
    val quantity: Int,
    val avgPrice: Double,
    val invested: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val unrealizedPnl: Double,
    val returnPct: Double
)

data class PortfolioData(
    val cash: Double,
    val totalInvested: Double,
    val currentValue: Double,
    val portfolioValue: Double,
    val unrealizedPnl: Double,
    val realizedPnl: Double,
    val totalPnl: Double,
    val returnPct: Double,
    val holdings: List<HoldingItem>
)

data class TransactionRecord(
    val id: Long,
    val orderId: Long?,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val realizedPnl: Double,
    val createdAt: String?
)

object TradingApi {

    suspend fun buy(
        symbol: String,
        quantity: Int,
        price: Double,
        clientOrderId: String? = null
    ): ApiResult<TradeOrderResult> {
        val json = JSONObject().apply {
            put("symbol", symbol.trim().uppercase())
            put("quantity", quantity)
            put("price", price)
            if (clientOrderId != null) {
                put("client_order_id", clientOrderId)
            }
        }

        return when (val res = ApiClient.postJsonObject("/api/trade/buy", json)) {
            is ApiResult.Success -> {
                try {
                    val obj = res.data
                    val txn = obj.optJSONObject("transaction")
                    val port = obj.optJSONObject("portfolio")
                    ApiResult.Success(
                        TradeOrderResult(
                            message = obj.optString("message", "Buy order completed"),
                            orderId = obj.optLong("order_id", -1L).takeIf { it > 0 },
                            symbol = txn?.optString("symbol", symbol) ?: symbol,
                            side = "BUY",
                            quantity = txn?.optInt("quantity", quantity) ?: quantity,
                            price = txn?.optDouble("price", price) ?: price,
                            total = txn?.optDouble("total", quantity * price) ?: (quantity * price),
                            cashBalance = port?.optDouble("cash_balance", 0.0) ?: 0.0
                        )
                    )
                } catch (e: Exception) {
                    ApiResult.Error("Failed to parse buy response: ${e.message}")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun sell(
        symbol: String,
        quantity: Int,
        price: Double,
        clientOrderId: String? = null
    ): ApiResult<TradeOrderResult> {
        val json = JSONObject().apply {
            put("symbol", symbol.trim().uppercase())
            put("quantity", quantity)
            put("price", price)
            if (clientOrderId != null) {
                put("client_order_id", clientOrderId)
            }
        }

        return when (val res = ApiClient.postJsonObject("/api/trade/sell", json)) {
            is ApiResult.Success -> {
                try {
                    val obj = res.data
                    val txn = obj.optJSONObject("transaction")
                    val port = obj.optJSONObject("portfolio")
                    ApiResult.Success(
                        TradeOrderResult(
                            message = obj.optString("message", "Sell order completed"),
                            orderId = obj.optLong("order_id", -1L).takeIf { it > 0 },
                            symbol = txn?.optString("symbol", symbol) ?: symbol,
                            side = "SELL",
                            quantity = txn?.optInt("quantity", quantity) ?: quantity,
                            price = txn?.optDouble("price", price) ?: price,
                            total = txn?.optDouble("total", quantity * price) ?: (quantity * price),
                            cashBalance = port?.optDouble("cash_balance", 0.0) ?: 0.0
                        )
                    )
                } catch (e: Exception) {
                    ApiResult.Error("Failed to parse sell response: ${e.message}")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun getPortfolio(): ApiResult<PortfolioData> {
        return when (val res = ApiClient.getJsonObject("/api/portfolio")) {
            is ApiResult.Success -> {
                try {
                    val obj = res.data
                    val holdingsList = mutableListOf<HoldingItem>()

                    if (obj.has("holdings_detail")) {
                        val arr = obj.getJSONArray("holdings_detail")
                        for (i in 0 until arr.length()) {
                            val h = arr.getJSONObject(i)
                            holdingsList.add(
                                HoldingItem(
                                    symbol = h.getString("symbol"),
                                    quantity = h.getInt("quantity"),
                                    avgPrice = h.getDouble("avg_price"),
                                    invested = h.getDouble("invested"),
                                    currentPrice = h.getDouble("current_price"),
                                    currentValue = h.getDouble("current_value"),
                                    unrealizedPnl = h.getDouble("unrealized_pnl"),
                                    returnPct = h.getDouble("return_pct")
                                )
                            )
                        }
                    } else if (obj.has("holdings")) {
                        // Fallback parsing from basic map
                        val hMap = obj.getJSONObject("holdings")
                        val aMap = obj.optJSONObject("average_prices")
                        for (key in hMap.keys()) {
                            val qty = hMap.getInt(key)
                            if (qty > 0) {
                                val avg = aMap?.optDouble(key, 0.0) ?: 0.0
                                val cur = 1000.0
                                holdingsList.add(
                                    HoldingItem(
                                        symbol = key,
                                        quantity = qty,
                                        avgPrice = avg,
                                        invested = qty * avg,
                                        currentPrice = cur,
                                        currentValue = qty * cur,
                                        unrealizedPnl = (cur - avg) * qty,
                                        returnPct = if (avg > 0) ((cur - avg) / avg * 100) else 0.0
                                    )
                                )
                            }
                        }
                    }

                    val cash = obj.optDouble("cash", obj.optDouble("cash_balance", 1_000_000.0))
                    val totalInvested = obj.optDouble("total_invested", 0.0)
                    val currentValue = obj.optDouble("current_value", obj.optDouble("market_value", 0.0))
                    val portfolioValue = obj.optDouble("portfolio_value", obj.optDouble("total_portfolio_value", cash + currentValue))
                    val unrealizedPnl = obj.optDouble("unrealized_pnl", 0.0)
                    val realizedPnl = obj.optDouble("realized_pnl", 0.0)
                    val totalPnl = obj.optDouble("total_pnl", unrealizedPnl + realizedPnl)
                    val returnPct = obj.optDouble("return_pct", obj.optDouble("unrealized_pnl_percent", 0.0))

                    ApiResult.Success(
                        PortfolioData(
                            cash = cash,
                            totalInvested = totalInvested,
                            currentValue = currentValue,
                            portfolioValue = portfolioValue,
                            unrealizedPnl = unrealizedPnl,
                            realizedPnl = realizedPnl,
                            totalPnl = totalPnl,
                            returnPct = returnPct,
                            holdings = holdingsList
                        )
                    )
                } catch (e: Exception) {
                    ApiResult.Error("Failed to parse portfolio: ${e.message}")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun getTransactions(
        side: String = "ALL",
        symbol: String? = null,
        sort: String = "newest"
    ): ApiResult<List<TransactionRecord>> {
        val params = mutableListOf("side=$side", "sort=$sort")
        if (!symbol.isNullOrBlank()) {
            params.add("symbol=${URLEncoder.encode(symbol.trim(), "UTF-8")}")
        }
        val query = params.joinToString("&")
        val path = "/api/transactions?$query"

        return when (val res = ApiClient.get(path)) {
            is ApiResult.Success -> {
                try {
                    val arr = JSONArray(res.data)
                    val list = mutableListOf<TransactionRecord>()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        list.add(
                            TransactionRecord(
                                id = item.optLong("id", i.toLong()),
                                orderId = item.optLong("order_id", -1L).takeIf { it > 0 },
                                symbol = item.getString("symbol"),
                                side = item.getString("side"),
                                quantity = item.getInt("quantity"),
                                price = item.getDouble("price"),
                                total = item.getDouble("total"),
                                realizedPnl = item.optDouble("realized_pnl", 0.0),
                                createdAt = item.optString("created_at", null)
                            )
                        )
                    }
                    ApiResult.Success(list)
                } catch (e: Exception) {
                    ApiResult.Error("Failed to parse transactions: ${e.message}")
                }
            }
            is ApiResult.Error -> res
        }
    }
}
