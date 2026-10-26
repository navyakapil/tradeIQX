package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
private val OfflineBannerBg = Color(0xFF2A1F0D)
private val OfflineText = Color(0xFFFFB74D)

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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        TradingStore.init(context)
        TradingStore.refresh(context)
    }

    val summary = TradingStore.portfolioSummary.value
    val cashBalance = summary?.cashBalance ?: TradingStore.balance.value
    val invested = summary?.totalInvested ?: run {
        TradingStore.holdings.entries.sumOf { (sym, qty) ->
            (TradingStore.averagePrices[sym] ?: 0.0) * qty
        }
    }
    val currentValue = summary?.currentValue ?: run {
        TradingStore.holdings.entries.sumOf { (sym, qty) ->
            (TradingStore.prices[sym] ?: 0.0) * qty
        }
    }
    val unrealizedPnl = summary?.unrealizedPnl ?: (currentValue - invested)
    val realizedPnl = summary?.realizedPnl ?: 0.0
    val totalPnl = summary?.totalPnl ?: (unrealizedPnl + realizedPnl)
    val returnPercent = summary?.returnPercent ?: if (invested > 0) (totalPnl / invested * 100.0) else 0.0
    val totalValue = cashBalance + currentValue

    val holdingsList: List<Holding> = summary?.holdings?.filter { it.quantity > 0 } ?: run {
        TradingStore.holdings.entries.filter { it.value > 0 }.map { (sym, qty) ->
            val avg = TradingStore.averagePrices[sym] ?: 0.0
            val cur = TradingStore.prices[sym] ?: 0.0
            val inv = avg * qty
            val curVal = cur * qty
            val un = curVal - inv
            val unPct = if (inv > 0) (un / inv * 100.0) else 0.0
            Holding(
                symbol = sym,
                quantity = qty,
                avgBuyPrice = avg,
                currentPrice = cur,
                investedValue = inv,
                currentValue = curVal,
                unrealizedPnl = un,
                unrealizedPnlPercent = unPct,
                realizedPnl = 0.0
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PortBg)
            .padding(16.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text("‹ Back", color = PortText)
        }

        // Offline Banner
        if (TradingStore.isOffline.value) {
            Surface(
                color = OfflineBannerBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡", fontSize = 12.sp)
                    Text(
                        "offline — showing last saved data",
                        color = OfflineText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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
            }

            if (TradingStore.isLoading.value) {
                CircularProgressIndicator(
                    color = PortGreen,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${if (unrealizedPnl >= 0) "+" else ""}${portMoney(unrealizedPnl)} unrealized P&L",
                        color = if (unrealizedPnl >= 0) PortGreen else PortRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${if (returnPercent >= 0) "+" else ""}${String.format(Locale.US, "%.2f", returnPercent)}%",
                        color = if (returnPercent >= 0) PortGreen else PortRed,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                        Text("Realized P&L", color = PortMuted, fontSize = 12.sp)
                        Text(
                            "${if (realizedPnl >= 0) "+" else ""}${portMoney(realizedPnl)}",
                            color = if (realizedPnl >= 0) PortGreen else PortRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text("Available cash", color = PortMuted, fontSize = 12.sp)
                        Text(
                            portMoney(cashBalance),
                            color = PortGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Holdings (${holdingsList.size})",
            color = PortText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        if (holdingsList.isEmpty()) {
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
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(holdingsList, key = { it.symbol }) { holding ->
                    val pnl = holding.unrealizedPnl
                    val pnlPercent = holding.unrealizedPnlPercent

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
                                    holding.symbol,
                                    color = PortText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    portMoney(holding.currentValue),
                                    color = PortText,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "${holding.quantity} shares · Avg. ${portMoney(holding.avgBuyPrice)} · Current ${portMoney(holding.currentPrice)}",
                                color = PortMuted,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Unrealized P&L: ${if (pnl >= 0) "+" else ""}${portMoney(pnl)}",
                                    color = if (pnl >= 0) PortGreen else PortRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${if (pnlPercent >= 0) "+" else ""}${String.format(Locale.US, "%.2f", pnlPercent)}%",
                                    color = if (pnlPercent >= 0) PortGreen else PortRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
