
package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val BuyBg = Color(0xFF071522)
private val BuyCard = Color(0xFF132838)
private val BuyGreen = Color(0xFF32D49A)
private val BuyText = Color(0xFFF8FAFC)
private val BuyMuted = Color(0xFF9FB0C0)

private fun buyMoney(value: Double): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

@Composable
fun BuyScreen(
    symbol: String,
    onBack: () -> Unit,
    onSell: () -> Unit,
    onOrderSuccess: () -> Unit = {}
) {
    var quantityText by remember(symbol) { mutableStateOf("1") }
    var localMessage by remember { mutableStateOf("") }

    val stock = symbol.uppercase()
    val price = TradingStore.prices[stock] ?: 0.0
    val quantity = quantityText.toIntOrNull() ?: 0
    val total = price * quantity
    val remaining = TradingStore.balance.value - total

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BuyBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Back", color = BuyText)
        }

        Text(
            "Buy $stock",
            color = BuyText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Paper trading · Simulated price",
            color = BuyMuted,
            fontSize = 13.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = BuyCard),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("CURRENT PRICE", color = BuyMuted, fontSize = 12.sp)
                Text(
                    buyMoney(price),
                    color = BuyText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                Text("Available virtual balance", color = BuyMuted)
                Text(
                    buyMoney(TradingStore.balance.value),
                    color = BuyGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "Quantity (shares)",
            color = BuyText,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    quantityText = (quantity - 1).coerceAtLeast(1).toString()
                }
            ) {
                Text("−")
            }

            OutlinedTextField(
                value = quantityText,
                onValueChange = {
                    if (it.length <= 7 &&
                        (it.isEmpty() || it.all(Char::isDigit))
                    ) {
                        quantityText = it
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Shares") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BuyText,
                    unfocusedTextColor = BuyText,
                    focusedLabelColor = BuyGreen,
                    unfocusedLabelColor = BuyMuted,
                    focusedBorderColor = BuyGreen,
                    unfocusedBorderColor = BuyMuted
                )
            )

            OutlinedButton(
                onClick = {
                    quantityText = (quantity + 1).toString()
                }
            ) {
                Text("+")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = BuyCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BuySummaryLine("Order amount", buyMoney(total))

                BuySummaryLine(
                    "Balance after order",
                    buyMoney(remaining)
                )
            }
        }

        if (localMessage.isNotBlank()) {
            Text(localMessage, color = BuyGreen)
        }

        Button(
            onClick = {
                val success = TradingStore.buy(stock, quantity)
                localMessage = TradingStore.message.value

                if (success) onOrderSuccess()
            },
            enabled = price > 0 &&
                    quantity > 0 &&
                    total <= TradingStore.balance.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BuyGreen,
                contentColor = Color(0xFF062017)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Confirm Buy Order", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onSell,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Sell $stock", color = BuyText)
        }
    }
}

@Composable
private fun BuySummaryLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = BuyMuted)
        Text(value, color = BuyText, fontWeight = FontWeight.Bold)
    }
}
