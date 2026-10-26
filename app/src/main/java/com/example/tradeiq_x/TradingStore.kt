package com.example.tradeiq_x

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

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

    const val INITIAL_BALANCE = 1_000_000.0
    private const val PREFS_NAME = "tradeiqx_trading_store"
    private const val KEY_PORTFOLIO_CACHE = "cached_portfolio_json"
    private const val KEY_TRANSACTIONS_CACHE = "cached_transactions_json"

    private val storeScope = CoroutineScope(Dispatchers.Main)

    var currentUserId = mutableStateOf("default_device_user")
        private set

    var balance = mutableStateOf(INITIAL_BALANCE)
        private set

    // Authoritative or fallback price table
    val prices = mutableStateMapOf(
        "RELIANCE" to 1450.25,
        "TCS" to 3920.50,
        "INFY" to 1620.30,
        "HDFCBANK" to 1780.90,
        "ICICIBANK" to 1245.60,
        "ITC" to 420.80,
        "SBIN" to 725.60,
        "WIPRO" to 290.40
    )

    // Share quantities currently owned
    val holdings = mutableStateMapOf<String, Int>()

    // Average purchase price for each holding
    val averagePrices = mutableStateMapOf<String, Double>()

    // Newest transactions appear first
    val transactions = mutableStateListOf<TradeTransaction>()

    // Detailed portfolio analytics
    var portfolioSummary = mutableStateOf<PortfolioSummary?>(null)
        private set

    var message = mutableStateOf("")
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isOffline = mutableStateOf(false)
        private set

    private var isInitialized = false

    /**
     * Initializes the TradingStore using the Android Device ID to guarantee
     * user-specific persistence across logouts, restarts, and app reinstalls.
     */
    fun init(context: Context) {
        if (!isInitialized) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (_: Exception) {
                null
            }
            if (!androidId.isNullOrBlank()) {
                currentUserId.value = androidId
            }
            loadFromCache(context)
            isInitialized = true
        }
    }

    /**
     * Hook to set an explicit user identifier without hard-coupling to the auth system.
     */
    fun setUser(userId: String) {
        val clean = userId.trim()
        if (clean.isNotBlank()) {
            currentUserId.value = clean
        }
    }

    suspend fun refresh(context: Context? = null) = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) { isLoading.value = true }
        try {
            val portJson = TradingApiClient.getPortfolio(currentUserId.value)
            val summary = PortfolioSummary.fromJson(portJson)

            val txJson = TradingApiClient.getTransactions(userId = currentUserId.value, limit = 100)
            val txList = mutableListOf<TradeTransaction>()
            for (i in 0 until txJson.length()) {
                val item = txJson.optJSONObject(i)
                if (item != null) {
                    val tx = Transaction.fromJson(item)
                    txList.add(
                        TradeTransaction(
                            id = tx.id,
                            symbol = tx.symbol,
                            type = tx.side,
                            quantity = tx.quantity,
                            price = tx.price,
                            total = tx.total,
                            timestamp = tx.timestamp
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                portfolioSummary.value = summary
                balance.value = summary.cashBalance

                holdings.clear()
                averagePrices.clear()
                for (h in summary.holdings) {
                    if (h.quantity > 0) {
                        holdings[h.symbol] = h.quantity
                        averagePrices[h.symbol] = h.avgBuyPrice
                        prices[h.symbol] = h.currentPrice
                    }
                }

                transactions.clear()
                transactions.addAll(txList)
                isOffline.value = false
            }

            if (context != null) {
                saveToCache(context, portJson.toString(), txJson.toString())
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                isOffline.value = true
                if (context != null) {
                    loadFromCache(context)
                }
            }
        } finally {
            withContext(Dispatchers.Main) { isLoading.value = false }
        }
    }

    suspend fun buyAsync(
        symbol: String,
        quantity: Int,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSymbol = symbol.trim().uppercase()
        val idempotencyKey = "buy_${currentUserId.value}_${cleanSymbol}_${System.currentTimeMillis()}_${UUID.randomUUID().hex16()}"

        withContext(Dispatchers.Main) {
            isLoading.value = true
            message.value = ""
        }

        try {
            val resJson = TradingApiClient.submitOrder(
                symbol = cleanSymbol,
                side = "BUY",
                quantity = quantity,
                userId = currentUserId.value,
                idempotencyKey = idempotencyKey
            )
            val result = OrderResult.fromJson(resJson, "Bought $quantity shares of $cleanSymbol.")

            withContext(Dispatchers.Main) {
                message.value = result.message
            }
            refresh(context)
            true
        } catch (e: TradingError) {
            withContext(Dispatchers.Main) {
                message.value = e.message ?: "Failed to place buy order."
            }
            false
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                message.value = "Server unreachable. Please check connection."
            }
            false
        } finally {
            withContext(Dispatchers.Main) { isLoading.value = false }
        }
    }

    suspend fun sellAsync(
        symbol: String,
        quantity: Int,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSymbol = symbol.trim().uppercase()
        val idempotencyKey = "sell_${currentUserId.value}_${cleanSymbol}_${System.currentTimeMillis()}_${UUID.randomUUID().hex16()}"

        withContext(Dispatchers.Main) {
            isLoading.value = true
            message.value = ""
        }

        try {
            val resJson = TradingApiClient.submitOrder(
                symbol = cleanSymbol,
                side = "SELL",
                quantity = quantity,
                userId = currentUserId.value,
                idempotencyKey = idempotencyKey
            )
            val result = OrderResult.fromJson(resJson, "Sold $quantity shares of $cleanSymbol.")

            withContext(Dispatchers.Main) {
                message.value = result.message
            }
            refresh(context)
            true
        } catch (e: TradingError) {
            withContext(Dispatchers.Main) {
                message.value = e.message ?: "Failed to place sell order."
            }
            false
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                message.value = "Server unreachable. Please check connection."
            }
            false
        } finally {
            withContext(Dispatchers.Main) { isLoading.value = false }
        }
    }

    /**
     * Backward-compatible buy method for existing screen invocations.
     */
    fun buy(symbol: String, quantity: Int): Boolean {
        val stock = symbol.uppercase()
        val price = prices[stock] ?: 0.0

        if (price <= 0.0) {
            message.value = "Stock price unavailable."
            return false
        }
        if (quantity <= 0) {
            message.value = "Enter a valid quantity."
            return false
        }
        val total = price * quantity
        if (total > balance.value) {
            message.value = "Insufficient virtual balance."
            return false
        }

        // Trigger network execution on storeScope
        storeScope.launch {
            buyAsync(symbol, quantity)
        }
        return true
    }

    /**
     * Backward-compatible sell method for existing screen invocations.
     */
    fun sell(symbol: String, quantity: Int): Boolean {
        val stock = symbol.uppercase()
        val price = prices[stock] ?: 0.0

        if (price <= 0.0) {
            message.value = "Stock price unavailable."
            return false
        }
        if (quantity <= 0) {
            message.value = "Enter a valid quantity."
            return false
        }
        val owned = holdings[stock] ?: 0
        if (quantity > owned) {
            message.value = "You don't own enough shares."
            return false
        }

        // Trigger network execution on storeScope
        storeScope.launch {
            sellAsync(symbol, quantity)
        }
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
        portfolioSummary.value = null
        message.value = ""
        isLoading.value = false
        isOffline.value = false
    }

    private fun saveToCache(context: Context, portfolioRaw: String, transactionsRaw: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PORTFOLIO_CACHE, portfolioRaw)
                .putString(KEY_TRANSACTIONS_CACHE, transactionsRaw)
                .apply()
        } catch (_: Exception) {}
    }

    private fun loadFromCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val portRaw = prefs.getString(KEY_PORTFOLIO_CACHE, null)
            val txRaw = prefs.getString(KEY_TRANSACTIONS_CACHE, null)

            if (!portRaw.isNullOrBlank()) {
                val summary = PortfolioSummary.fromJson(JSONObject(portRaw))
                portfolioSummary.value = summary
                balance.value = summary.cashBalance
                holdings.clear()
                averagePrices.clear()
                for (h in summary.holdings) {
                    if (h.quantity > 0) {
                        holdings[h.symbol] = h.quantity
                        averagePrices[h.symbol] = h.avgBuyPrice
                    }
                }
            }

            if (!txRaw.isNullOrBlank()) {
                val array = org.json.JSONArray(txRaw)
                transactions.clear()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        val tx = Transaction.fromJson(item)
                        transactions.add(
                            TradeTransaction(
                                id = tx.id,
                                symbol = tx.symbol,
                                type = tx.side,
                                quantity = tx.quantity,
                                price = tx.price,
                                total = tx.total,
                                timestamp = tx.timestamp
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun UUID.hex16(): String = toString().replace("-", "").take(16)
}
