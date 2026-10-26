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
private val BuyRed = Color(0xFFFF5364)
private val BuyText = Color(0xFFF8FAFC)
private val BuyMuted = Color(0xFF9FB0C0)

private fun buyMoney(value: Double): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

@Composable
fun BuyScreen(
    symbol: String = "RELIANCE",
    onBack: () -> Unit = {},
    onSell: () -> Unit = {},
    onOrderSuccess: () -> Unit = {},
    price: Double = TradingStore.prices[symbol.uppercase()] ?: 1450.25
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(symbol) {
        TradingStore.init(context)
        TradingStore.refresh(context)
    }

    var quantityText by remember(symbol) { mutableStateOf("1") }
    var localMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val stock = symbol.uppercase()
    val currentPrice = TradingStore.prices[stock] ?: price
    val quantity = quantityText.toIntOrNull() ?: 0
    val total = currentPrice * quantity
    val currentBalance = TradingStore.balance.value
    val remaining = currentBalance - total

    val isQuantityValid = quantity > 0
    val isFundsSufficient = total <= currentBalance

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
                    buyMoney(currentPrice),
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
                Text("−")
            }

            OutlinedTextField(
                value = quantityText,
                onValueChange = {
                    if (it.length <= 7 && (it.isEmpty() || it.all(Char::isDigit))) {
                        quantityText = it
                    }
                },
                enabled = !isSubmitting,
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
                },
                enabled = !isSubmitting
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

        // Inline validation error cues
        if (quantityText.isNotBlank() && quantity <= 0) {
            Text("Please enter a valid quantity of shares.", color = BuyRed, fontSize = 13.sp)
        } else if (!isFundsSufficient) {
            Text(
                "Insufficient virtual balance. Required: ${buyMoney(total)}",
                color = BuyRed,
                fontSize = 13.sp
            )
        }

        if (localMessage.isNotBlank()) {
            Text(
                localMessage,
                color = if (localMessage.contains("Bought", ignoreCase = true)) BuyGreen else BuyRed,
                fontSize = 14.sp
            )
        }

        Button(
            onClick = {
                scope.launch {
                    isSubmitting = true
                    val success = TradingStore.buyAsync(stock, quantity, context)
                    localMessage = TradingStore.message.value
                    isSubmitting = false
                    if (success) {
                        onOrderSuccess()
                    }
                }
            },
            enabled = !isSubmitting &&
                    currentPrice > 0 &&
                    isQuantityValid &&
                    isFundsSufficient,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BuyGreen,
                contentColor = Color(0xFF062017)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color(0xFF062017),
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Confirm Buy Order", fontWeight = FontWeight.Bold)
            }
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
