package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private val PortBg = Color(0xFF071522)
private val PortCard = Color(0xFF132838)
private val PortGreen = Color(0xFF32D49A)
private val PortRed = Color(0xFFFF5364)
private val PortText = Color(0xFFF8FAFC)
private val PortMuted = Color(0xFF9FB0C0)
private val PortBorder = Color(0xFF1E3A4B)

private fun portMoney(value: Double): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

private fun portPercent(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return "$sign${String.format(Locale.US, "%.2f", value)}%"
}

@Composable
fun PortfolioScreen(
    onBackClick: () -> Unit = {},
    onTransactionsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val portfolio = TradingStore.portfolioState.value
    val holdings = TradingStore.holdingsDetailList
    val isSyncing = TradingStore.isSyncing.value
    val syncError = TradingStore.syncError.value

    LaunchedEffect(Unit) {
        TradingStore.init(context)
        TradingStore.refreshPortfolio(context)
    }

    val cash = portfolio?.cash ?: TradingStore.balance.value
    val totalInvested = portfolio?.totalInvested ?: 0.0
    val currentValue = portfolio?.currentValue ?: 0.0
    val portfolioValue = portfolio?.portfolioValue ?: (cash + currentValue)
    val unrealizedPnl = portfolio?.unrealizedPnl ?: (currentValue - totalInvested)
    val realizedPnl = portfolio?.realizedPnl ?: 0.0
    val returnPct = portfolio?.returnPct ?: (if (totalInvested > 0) (unrealizedPnl / totalInvested * 100.0) else 0.0)

    Column(
        Modifier
            .fillMaxSize()
            .background(PortBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("‹ Back", color = PortText, fontSize = 16.sp)
            }

            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = PortGreen,
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(onClick = { scope.launch { TradingStore.refreshPortfolio(context) } }) {
                    Text("↻ Refresh", color = PortGreen, fontSize = 13.sp)
                }
            }
        }

        Text(
            "My Portfolio",
            color = PortText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Persistent paper-trading performance & analytics",
            color = PortMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(14.dp))

        if (syncError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF381519)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sync error: $syncError (showing cached data)",
                        color = PortRed,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { scope.launch { TradingStore.refreshPortfolio(context) } }) {
                        Text("Retry", color = PortGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SUMMARY CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = PortCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(
                    "TOTAL PORTFOLIO VALUE",
                    color = PortMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    portMoney(portfolioValue),
                    color = PortText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${if (unrealizedPnl >= 0) "+" else ""}${portMoney(unrealizedPnl)} (${portPercent(returnPct)})",
                        color = if (unrealizedPnl >= 0) PortGreen else PortRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(" • Unrealized P&L", color = PortMuted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = PortBorder)
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Invested", color = PortMuted, fontSize = 11.sp)
                        Text(portMoney(totalInvested), color = PortText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Current Holdings Value", color = PortMuted, fontSize = 11.sp)
                        Text(portMoney(currentValue), color = PortText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Column(Modifier.weight(1f)) {
                        Text("Available Cash", color = PortMuted, fontSize = 11.sp)
                        Text(portMoney(cash), color = PortGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Realized P&L", color = PortMuted, fontSize = 11.sp)
                        Text(
                            "${if (realizedPnl >= 0) "+" else ""}${portMoney(realizedPnl)}",
                            color = if (realizedPnl >= 0) PortGreen else PortRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Holdings (${holdings.size})",
            color = PortText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        if (holdings.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PortCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No holdings yet 📈", color = PortText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You currently have no open stock positions. Start paper trading by placing a buy order from Market Watch!",
                        color = PortMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(holdings, key = { it.symbol }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PortCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        item.symbol,
                                        color = PortText,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${item.quantity} shares • Avg. ${portMoney(item.avgPrice)}",
                                        color = PortMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        portMoney(item.currentValue),
                                        color = PortText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${if (item.unrealizedPnl >= 0) "+" else ""}${portMoney(item.unrealizedPnl)} (${portPercent(item.returnPct)})",
                                        color = if (item.unrealizedPnl >= 0) PortGreen else PortRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onTransactionsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PortGreen,
                contentColor = Color(0xFF062017)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("View Transactions →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
