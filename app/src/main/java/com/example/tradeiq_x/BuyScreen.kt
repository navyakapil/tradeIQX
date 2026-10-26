package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
    stockPrice: Double? = null,
    onBack: () -> Unit,
    onSell: () -> Unit,
    onOrderSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var quantityText by remember(symbol) { mutableStateOf("1") }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val stock = symbol.uppercase()
    val price = stockPrice ?: TradingStore.prices[stock] ?: 1000.0
    val quantity = quantityText.toIntOrNull() ?: 0
    val total = price * quantity
    val currentBalance = TradingStore.balance.value
    val remaining = currentBalance - total

    LaunchedEffect(Unit) {
        TradingStore.init(context)
        TradingStore.refreshPortfolio(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BuyBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack, enabled = !isSubmitting) {
            Text("‹ Back", color = BuyText)
        }

        Text(
            "Buy $stock",
            color = BuyText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Paper trading • Real-time backend sync",
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
                    buyMoney(currentBalance),
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
                },
                enabled = !isSubmitting
            ) {
                Text("-", color = BuyText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = quantityText,
                onValueChange = {
                    if (it.length <= 7 && (it.isEmpty() || it.all(Char::isDigit))) {
                        quantityText = it
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isSubmitting,
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
                },
                enabled = !isSubmitting
            ) {
                Text("+", color = BuyText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

        if (statusMessage.isNotBlank()) {
            Text(
                statusMessage,
                color = if (isError) Color(0xFFFF5364) else BuyGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = {
                if (isSubmitting) return@Button
                isSubmitting = true
                statusMessage = ""
                isError = false

                scope.launch {
                    val clientOrderId = "buy_${stock}_${System.currentTimeMillis()}_${(1000..9999).random()}"
                    val (success, msg) = TradingStore.executeBuy(stock, quantity, price, context, clientOrderId)
                    isSubmitting = false
                    statusMessage = msg
                    isError = !success
                    if (success) {
                        kotlinx.coroutines.delay(400)
                        onOrderSuccess()
                    }
                }
            },
            enabled = price > 0 && quantity > 0 && total <= currentBalance && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BuyGreen,
                disabledContainerColor = Color(0xFF1E3A32),
                contentColor = Color(0xFF062017)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF062017),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Confirm Buy Order", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onSell,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
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
