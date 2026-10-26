package com.example.tradeiq_x

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TradeTransaction(
    val id: Long,
    val symbol: String,
    val type: String,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val timestamp: Long = System.currentTimeMillis()
)

object TradingStore {

    private const val INITIAL_BALANCE = 1_000_000.0
    private const val PREFS_NAME = "tradeiqx_portfolio_cache"
    private const val KEY_PORTFOLIO_CACHE = "portfolio_json"
    private const val KEY_TRANSACTIONS_CACHE = "transactions_json"

    private var sharedPrefs: SharedPreferences? = null

    var balance = mutableStateOf(INITIAL_BALANCE)
        private set

    val portfolioState = mutableStateOf<PortfolioData?>(null)
    val holdingsDetailList = mutableStateListOf<HoldingItem>()
    val transactionsList = mutableStateListOf<TransactionRecord>()

    val isSyncing = mutableStateOf(false)
    val syncError = mutableStateOf<String?>(null)

    // Static prices for default lookup and fallback
    val prices = mapOf(
        "RELIANCE" to 1450.25,
        "TCS" to 3920.50,
        "INFY" to 1620.30,
        "HDFCBANK" to 1780.90,
        "ICICIBANK" to 1245.60,
        "ITC" to 420.80,
        "SBIN" to 725.60,
        "WIPRO" to 290.40,
        "TATAMOTORS" to 980.50,
        "BHARTIARTL" to 1560.00,
        "KOTAKBANK" to 1750.00,
        "LT" to 3600.00
    )

    // Legacy maps for backward compatibility
    val holdings = mutableStateMapOf<String, Int>()
    val averagePrices = mutableStateMapOf<String, Double>()
    val transactions = mutableStateListOf<TradeTransaction>()

    var message = mutableStateOf("")
        private set

    fun init(context: Context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromCache()
        }
    }

    private fun loadFromCache() {
        val prefs = sharedPrefs ?: return
        val portJsonStr = prefs.getString(KEY_PORTFOLIO_CACHE, null)
        if (!portJsonStr.isNullOrBlank()) {
            try {
                val obj = JSONObject(portJsonStr)
                val cash = obj.optDouble("cash", INITIAL_BALANCE)
                balance.value = cash

                val hList = mutableListOf<HoldingItem>()
                val arr = obj.optJSONArray("holdings_detail")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val h = arr.getJSONObject(i)
                        hList.add(
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
                }

                holdingsDetailList.clear()
                holdingsDetailList.addAll(hList)
                holdings.clear()
                averagePrices.clear()
                hList.forEach {
                    holdings[it.symbol] = it.quantity
                    averagePrices[it.symbol] = it.avgPrice
                }

                val totalInv = obj.optDouble("total_invested", 0.0)
                val currVal = obj.optDouble("current_value", 0.0)
                val portVal = obj.optDouble("portfolio_value", cash + currVal)
                val unReal = obj.optDouble("unrealized_pnl", 0.0)
                val real = obj.optDouble("realized_pnl", 0.0)
                val totPnl = obj.optDouble("total_pnl", 0.0)
                val retPct = obj.optDouble("return_pct", 0.0)

                portfolioState.value = PortfolioData(
                    cash = cash,
                    totalInvested = totalInv,
                    currentValue = currVal,
                    portfolioValue = portVal,
                    unrealizedPnl = unReal,
                    realizedPnl = real,
                    totalPnl = totPnl,
                    returnPct = retPct,
                    holdings = hList
                )
            } catch (_: Exception) {}
        }

        val txnJsonStr = prefs.getString(KEY_TRANSACTIONS_CACHE, null)
        if (!txnJsonStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(txnJsonStr)
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
                transactionsList.clear()
                transactionsList.addAll(list)
            } catch (_: Exception) {}
        }
    }

    private fun savePortfolioToCache(data: PortfolioData) {
        val prefs = sharedPrefs ?: return
        try {
            val obj = JSONObject().apply {
                put("cash", data.cash)
                put("total_invested", data.totalInvested)
                put("current_value", data.currentValue)
                put("portfolio_value", data.portfolioValue)
                put("unrealized_pnl", data.unrealizedPnl)
                put("realized_pnl", data.realizedPnl)
                put("total_pnl", data.totalPnl)
                put("return_pct", data.returnPct)
                val arr = JSONArray()
                data.holdings.forEach { h ->
                    arr.put(
                        JSONObject().apply {
                            put("symbol", h.symbol)
                            put("quantity", h.quantity)
                            put("avg_price", h.avgPrice)
                            put("invested", h.invested)
                            put("current_price", h.currentPrice)
                            put("current_value", h.currentValue)
                            put("unrealized_pnl", h.unrealizedPnl)
                            put("return_pct", h.returnPct)
                        }
                    )
                }
                put("holdings_detail", arr)
            }
            prefs.edit().putString(KEY_PORTFOLIO_CACHE, obj.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun saveTransactionsToCache(list: List<TransactionRecord>) {
        val prefs = sharedPrefs ?: return
        try {
            val arr = JSONArray()
            list.forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        if (t.orderId != null) put("order_id", t.orderId)
                        put("symbol", t.symbol)
                        put("side", t.side)
                        put("quantity", t.quantity)
                        put("price", t.price)
                        put("total", t.total)
                        put("realized_pnl", t.realizedPnl)
                        if (t.createdAt != null) put("created_at", t.createdAt)
                    }
                )
            }
            prefs.edit().putString(KEY_TRANSACTIONS_CACHE, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    suspend fun refreshPortfolio(context: Context? = null): Boolean {
        if (context != null) init(context)
        isSyncing.value = true
        syncError.value = null

        return when (val res = TradingApi.getPortfolio()) {
            is ApiResult.Success -> {
                val data = res.data
                portfolioState.value = data
                balance.value = data.cash
                holdingsDetailList.clear()
                holdingsDetailList.addAll(data.holdings)

                holdings.clear()
                averagePrices.clear()
                data.holdings.forEach {
                    holdings[it.symbol] = it.quantity
                    averagePrices[it.symbol] = it.avgPrice
                }
                savePortfolioToCache(data)
                isSyncing.value = false
                true
            }
            is ApiResult.Error -> {
                syncError.value = res.message
                isSyncing.value = false
                false
            }
        }
    }

    suspend fun refreshTransactions(
        context: Context? = null,
        side: String = "ALL",
        symbol: String? = null,
        sort: String = "newest"
    ): Boolean {
        if (context != null) init(context)
        isSyncing.value = true
        syncError.value = null

        return when (val res = TradingApi.getTransactions(side, symbol, sort)) {
            is ApiResult.Success -> {
                transactionsList.clear()
                transactionsList.addAll(res.data)
                saveTransactionsToCache(res.data)
                isSyncing.value = false
                true
            }
            is ApiResult.Error -> {
                syncError.value = res.message
                isSyncing.value = false
                false
            }
        }
    }

    suspend fun executeBuy(
        symbol: String,
        quantity: Int,
        price: Double,
        context: Context? = null
    ): Pair<Boolean, String> {
        if (context != null) init(context)
        isSyncing.value = true
        syncError.value = null

        return when (val res = TradingApi.buy(symbol, quantity, price)) {
            is ApiResult.Success -> {
                message.value = res.data.message
                refreshPortfolio(context)
                refreshTransactions(context)
                isSyncing.value = false
                Pair(true, res.data.message)
            }
            is ApiResult.Error -> {
                syncError.value = res.message
                message.value = res.message
                isSyncing.value = false
                Pair(false, res.message)
            }
        }
    }

    suspend fun executeSell(
        symbol: String,
        quantity: Int,
        price: Double,
        context: Context? = null
    ): Pair<Boolean, String> {
        if (context != null) init(context)
        isSyncing.value = true
        syncError.value = null

        return when (val res = TradingApi.sell(symbol, quantity, price)) {
            is ApiResult.Success -> {
                message.value = res.data.message
                refreshPortfolio(context)
                refreshTransactions(context)
                isSyncing.value = false
                Pair(true, res.data.message)
            }
            is ApiResult.Error -> {
                syncError.value = res.message
                message.value = res.message
                isSyncing.value = false
                Pair(false, res.message)
            }
        }
    }

    // Synchronous fallback methods for backward compatibility
    fun buy(symbol: String, quantity: Int): Boolean {
        val stock = symbol.uppercase()
        val price = prices[stock] ?: 1000.0

        if (quantity <= 0) {
            message.value = "Enter a valid quantity."
            return false
        }

        val total = price * quantity
        if (total > balance.value) {
            message.value = "Insufficient virtual balance."
            return false
        }

        val oldQty = holdings[stock] ?: 0
        val oldAvg = averagePrices[stock] ?: 0.0
        val newQty = oldQty + quantity
        averagePrices[stock] = ((oldQty * oldAvg) + total) / newQty
        holdings[stock] = newQty
        balance.value -= total

        transactions.add(
            0,
            TradeTransaction(
                id = System.nanoTime(),
                symbol = stock,
                type = "BUY",
                quantity = quantity,
                price = price,
                total = total
            )
        )
        message.value = "Bought $quantity shares of $stock."
        return true
    }

    fun sell(symbol: String, quantity: Int): Boolean {
        val stock = symbol.uppercase()
        val price = prices[stock] ?: 1000.0

        if (quantity <= 0) {
            message.value = "Enter a valid quantity."
            return false
        }

        val owned = holdings[stock] ?: 0
        if (quantity > owned) {
            message.value = "You don't own enough shares."
            return false
        }

        val total = price * quantity
        val remaining = owned - quantity
        if (remaining == 0) {
            holdings.remove(stock)
            averagePrices.remove(stock)
        } else {
            holdings[stock] = remaining
        }
        balance.value += total

        transactions.add(
            0,
            TradeTransaction(
                id = System.nanoTime(),
                symbol = stock,
                type = "SELL",
                quantity = quantity,
                price = price,
                total = total
            )
        )
        message.value = "Sold $quantity shares of $stock."
        return true
    }

    fun resetMessage() {
        message.value = ""
    }

    fun resetForTesting() {
        balance.value = INITIAL_BALANCE
        holdings.clear()
        averagePrices.clear()
        transactions.clear()
        holdingsDetailList.clear()
        transactionsList.clear()
        portfolioState.value = null
        message.value = ""
        syncError.value = null
    }
}
