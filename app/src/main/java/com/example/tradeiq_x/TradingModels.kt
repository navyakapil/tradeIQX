package com.example.tradeiq_x

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class OrderResult(
    val success: Boolean,
    val orderId: String,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val price: Double,
    val totalValue: Double,
    val message: String
) {
    companion object {
        fun fromJson(json: JSONObject, message: String = "Order executed successfully"): OrderResult {
            return OrderResult(
                success = json.optString("status", "COMPLETED") == "COMPLETED",
                orderId = json.optString("id", ""),
                symbol = json.optString("symbol", ""),
                side = json.optString("side", "BUY"),
                quantity = json.optInt("quantity", 0),
                price = json.optDouble("price", 0.0),
                totalValue = json.optDouble("total_value", 0.0),
                message = message
            )
        }
    }
}

data class Holding(
    val symbol: String,
    val quantity: Int,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val investedValue: Double,
    val currentValue: Double,
    val unrealizedPnl: Double,
    val unrealizedPnlPercent: Double,
    val realizedPnl: Double
) {
    companion object {
        fun fromJson(json: JSONObject): Holding {
            return Holding(
                symbol = json.optString("symbol", ""),
                quantity = json.optInt("quantity", 0),
                avgBuyPrice = json.optDouble("avg_buy_price", 0.0),
                currentPrice = json.optDouble("current_price", 0.0),
                investedValue = json.optDouble("invested_value", 0.0),
                currentValue = json.optDouble("current_value", 0.0),
                unrealizedPnl = json.optDouble("unrealized_pnl", 0.0),
                unrealizedPnlPercent = json.optDouble("unrealized_pnl_percent", 0.0),
                realizedPnl = json.optDouble("realized_pnl", 0.0)
            )
        }
    }
}

data class PortfolioSummary(
    val userId: String,
    val cashBalance: Double,
    val totalInvested: Double,
    val currentValue: Double,
    val portfolioValue: Double,
    val unrealizedPnl: Double,
    val realizedPnl: Double,
    val totalPnl: Double,
    val returnPercent: Double,
    val holdings: List<Holding>
) {
    companion object {
        fun fromJson(json: JSONObject): PortfolioSummary {
            val holdingsJson = json.optJSONArray("holdings") ?: JSONArray()
            val holdingsList = mutableListOf<Holding>()
            for (i in 0 until holdingsJson.length()) {
                val item = holdingsJson.optJSONObject(i)
                if (item != null) {
                    holdingsList.add(Holding.fromJson(item))
                }
            }
            return PortfolioSummary(
                userId = json.optString("user_id", "default_user"),
                cashBalance = json.optDouble("cash_balance", 1_000_000.0),
                totalInvested = json.optDouble("total_invested", 0.0),
                currentValue = json.optDouble("current_value", 0.0),
                portfolioValue = json.optDouble("portfolio_value", 1_000_000.0),
                unrealizedPnl = json.optDouble("unrealized_pnl", 0.0),
                realizedPnl = json.optDouble("realized_pnl", 0.0),
                totalPnl = json.optDouble("total_pnl", 0.0),
                returnPercent = json.optDouble("return_percent", 0.0),
                holdings = holdingsList
            )
        }
    }
}

data class Transaction(
    val id: Long,
    val orderId: String,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val timestamp: Long
) {
    companion object {
        private fun parseIsoTimestamp(isoString: String?): Long {
            if (isoString.isNullOrBlank()) return System.currentTimeMillis()
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val cleaned = if (isoString.contains(".")) isoString.substringBefore(".") else isoString
                parser.parse(cleaned)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }

        fun fromJson(json: JSONObject): Transaction {
            val isoDate = json.optString("executed_at")
            val ts = parseIsoTimestamp(isoDate)
            return Transaction(
                id = json.optLong("id", System.nanoTime()),
                orderId = json.optString("order_id", ""),
                symbol = json.optString("symbol", ""),
                side = json.optString("side", "BUY"),
                quantity = json.optInt("quantity", 0),
                price = json.optDouble("price", 0.0),
                total = json.optDouble("total_value", 0.0),
                timestamp = ts
            )
        }
    }
}

class TradingError(val statusCode: Int, message: String) : Exception(message)
