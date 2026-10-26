
package com.example.tradeiq_x

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

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

    var balance = mutableStateOf(INITIAL_BALANCE)
        private set

    // Simulated prices for initial testing only.
    val prices = mapOf(
        "RELIANCE" to 1450.25,
        "TCS" to 3200.00,
        "INFY" to 1500.00,
        "HDFCBANK" to 1700.00
    )

    // Share quantities currently owned.
    val holdings = mutableStateMapOf<String, Int>()

    // Average purchase price for each holding.
    val averagePrices = mutableStateMapOf<String, Double>()

    // Newest transactions appear first.
    val transactions = mutableStateListOf<TradeTransaction>()

    var message = mutableStateOf("")
        private set

    fun buy(symbol: String, quantity: Int): Boolean {

        val stock = symbol.uppercase()
        val price = prices[stock]

        if (price == null) {
            message.value = "Stock price unavailable."
            return false
        }

        if (quantity <= 0) {
            message.value = "Enter a valid quantity."
            return false
        }

        val oldQuantity = holdings[stock] ?: 0

        // Prevent integer overflow when adding shares.
        if (quantity > Int.MAX_VALUE - oldQuantity) {
            message.value = "Quantity exceeds the allowed limit."
            return false
        }

        val total = price * quantity

        if (total > balance.value) {
            message.value = "Insufficient virtual balance."
            return false
        }

        val oldAverage = averagePrices[stock] ?: 0.0
        val newQuantity = oldQuantity + quantity

        averagePrices[stock] =
            ((oldQuantity * oldAverage) + total) / newQuantity

        holdings[stock] = newQuantity
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
        val price = prices[stock]

        if (price == null) {
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
        message.value = ""
    }
}
