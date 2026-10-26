
package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val PortBg = Color(0xFF071522)
private val PortCard = Color(0xFF132838)
private val PortGreen = Color(0xFF32D49A)
private val PortRed = Color(0xFFFF5364)
private val PortText = Color(0xFFF8FAFC)
private val PortMuted = Color(0xFF9FB0C0)

private fun portMoney(value: Double): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

@Composable
fun PortfolioScreen(
    onBackClick: () -> Unit = {},
    onTransactionsClick: () -> Unit = {}
) {
    val holdings = TradingStore.holdings.toMap()
        .filterValues { it > 0 }

    val invested = holdings.entries.sumOf { (symbol, qty) ->
        (TradingStore.averagePrices[symbol] ?: 0.0) * qty
    }

    val currentValue = holdings.entries.sumOf { (symbol, qty) ->
        (TradingStore.prices[symbol] ?: 0.0) * qty
    }

    val profit = currentValue - invested
    val totalValue = TradingStore.balance.value + currentValue

    Column(
        Modifier
            .fillMaxSize()
            .background(PortBg)
            .padding(16.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text("‹ Back", color = PortText)
        }

        Text(
            "My Portfolio",
            color = PortText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Your paper-trading performance",
            color = PortMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PortCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(
                    "TOTAL PORTFOLIO VALUE",
                    color = PortMuted,
                    fontSize = 12.sp
                )

                Text(
                    portMoney(totalValue),
                    color = PortText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${if (profit >= 0) "+" else ""}${portMoney(profit)} unrealized P&L",
                    color = if (profit >= 0) PortGreen else PortRed
                )

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Invested", color = PortMuted, fontSize = 12.sp)
                        Text(
                            portMoney(invested),
                            color = PortText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text("Available cash", color = PortMuted, fontSize = 12.sp)
                        Text(
                            portMoney(TradingStore.balance.value),
                            color = PortGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Holdings (${holdings.size})",
            color = PortText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        if (holdings.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PortCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("No holdings yet", color = PortText, fontWeight = FontWeight.Bold)
                    Text(
                        "Buy shares from Market Watch to see them here.",
                        color = PortMuted
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(holdings.entries.toList(), key = { it.key }) { entry ->
                    val symbol = entry.key
                    val qty = entry.value
                    val avg = TradingStore.averagePrices[symbol] ?: 0.0
                    val price = TradingStore.prices[symbol] ?: 0.0
                    val pnl = (price - avg) * qty

                    Card(
                        colors = CardDefaults.cardColors(containerColor = PortCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    symbol,
                                    color = PortText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    portMoney(price * qty),
                                    color = PortText,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "$qty shares · Avg. ${portMoney(avg)} · Current ${portMoney(price)}",
                                color = PortMuted,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "P&L: ${portMoney(pnl)}",
                                color = if (pnl >= 0) PortGreen else PortRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onTransactionsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PortGreen,
                contentColor = Color(0xFF062017)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("View Transactions →", fontWeight = FontWeight.Bold)
        }
    }
}
