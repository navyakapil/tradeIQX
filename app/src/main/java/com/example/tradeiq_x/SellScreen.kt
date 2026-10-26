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

private val SellBg = Color(0xFF071522)
private val SellCard = Color(0xFF132838)
private val SellRed = Color(0xFFFF5364)
private val SellText = Color(0xFFF8FAFC)
private val SellMuted = Color(0xFF9FB0C0)

private fun sellMoney(value: Double): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

@Composable
fun SellScreen(
    symbol: String,
    stockPrice: Double? = null,
    onBack: () -> Unit,
    onOrderSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val stock = symbol.uppercase()
    var quantityText by remember(stock) { mutableStateOf("1") }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val price = stockPrice ?: TradingStore.prices[stock] ?: 1000.0
    val owned = TradingStore.holdings[stock] ?: 0
    val quantity = quantityText.toIntOrNull() ?: 0
    val total = price * quantity
    val remaining = owned - quantity

    LaunchedEffect(Unit) {
        TradingStore.init(context)
        TradingStore.refreshPortfolio(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SellBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack, enabled = !isSubmitting) {
            Text("‹ Back", color = SellText)
        }

        Text(
            "Sell $stock",
            color = SellText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Paper trading • Real-time backend sync",
            color = SellMuted,
            fontSize = 13.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SellCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("CURRENT PRICE", color = SellMuted, fontSize = 12.sp)

                Text(
                    sellMoney(price),
                    color = SellText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                Text("Shares owned", color = SellMuted)

                Text(
                    "$owned shares",
                    color = SellText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "Quantity to sell",
            color = SellText,
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
                Text("-", color = SellText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    focusedTextColor = SellText,
                    unfocusedTextColor = SellText,
                    focusedLabelColor = SellRed,
                    unfocusedLabelColor = SellMuted,
                    focusedBorderColor = SellRed,
                    unfocusedBorderColor = SellMuted
                )
            )

            OutlinedButton(
                onClick = {
                    quantityText = (quantity + 1).coerceAtMost(owned).toString()
                },
                enabled = !isSubmitting && quantity < owned
            ) {
                Text("+", color = SellText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SellCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SellSummaryLine("Estimated proceeds", sellMoney(total))
                SellSummaryLine(
                    "Shares after sale",
                    remaining.coerceAtLeast(0).toString()
                )
            }
        }

        if (owned == 0) {
            Text("You do not own any shares of $stock.", color = SellRed, fontSize = 13.sp)
        }

        if (statusMessage.isNotBlank()) {
            Text(
                statusMessage,
                color = if (isError) SellRed else Color(0xFF32D49A),
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
                    val (success, msg) = TradingStore.executeSell(stock, quantity, price, context)
                    isSubmitting = false
                    statusMessage = msg
                    isError = !success
                    if (success) {
                        kotlinx.coroutines.delay(400)
                        onOrderSuccess()
                    }
                }
            },
            enabled = price > 0 && quantity > 0 && quantity <= owned && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SellRed,
                disabledContainerColor = Color(0xFF3A1C20),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Confirm Sell Order", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SellSummaryLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SellMuted)
        Text(value, color = SellText, fontWeight = FontWeight.Bold)
    }
}
