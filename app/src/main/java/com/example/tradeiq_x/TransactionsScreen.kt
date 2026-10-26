package com.example.tradeiq_x

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TxBg = Color(0xFF071522)
private val TxCard = Color(0xFF132838)
private val TxGreen = Color(0xFF32D49A)
private val TxRed = Color(0xFFFF5364)
private val TxText = Color(0xFFF8FAFC)
private val TxMuted = Color(0xFF9FB0C0)
private val TxBorder = Color(0xFF1E3A4B)

private fun txMoney(value: Double): String = Formatters.formatInr(value)

private fun formatTimestamp(raw: String?): String = Formatters.formatTransactionDate(raw)

@Composable
fun TransactionsScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sideFilter by rememberSaveable { mutableStateOf("ALL") }
    var sortOption by rememberSaveable { mutableStateOf("newest") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val transactions = TradingStore.transactionsList
    val isSyncing = TradingStore.isSyncing.value
    val syncError = TradingStore.syncError.value

    LaunchedEffect(sideFilter, sortOption, searchQuery) {
        TradingStore.init(context)
        TradingStore.refreshTransactions(
            context = context,
            side = sideFilter,
            symbol = searchQuery.ifBlank { null },
            sort = sortOption
        )
    }

    BackHandler(onBack = onBackClick)

    Column(
        Modifier
            .fillMaxSize()
            .background(TxBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("‹ Back", color = TxText, fontSize = 16.sp)
            }

            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TxGreen,
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(
                    onClick = {
                        scope.launch {
                            TradingStore.refreshTransactions(
                                context,
                                sideFilter,
                                searchQuery.ifBlank { null },
                                sortOption
                            )
                        }
                    }
                ) {
                    Text("↻ Refresh", color = TxGreen, fontSize = 13.sp)
                }
            }
        }

        Text(
            "Transactions",
            color = TxText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Your executed paper trading order history",
            color = TxMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(14.dp))

        // SEARCH INPUT
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search by symbol (e.g. Reliance, TCS)...", color = TxMuted, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TxText,
                unfocusedTextColor = TxText,
                focusedBorderColor = TxGreen,
                unfocusedBorderColor = TxBorder,
                focusedContainerColor = TxCard,
                unfocusedContainerColor = TxCard
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(10.dp))

        // FILTER AND SORT ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Side filters
            listOf("ALL", "BUY", "SELL").forEach { option ->
                FilterChip(
                    selected = sideFilter == option,
                    onClick = { sideFilter = option },
                    label = { Text(option, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TxGreen,
                        selectedLabelColor = Color(0xFF062017),
                        labelColor = TxText
                    )
                )
            }

            Text("|", color = TxBorder)

            // Sort chips
            val sortOptions = listOf(
                "newest" to "Newest",
                "oldest" to "Oldest",
                "amount_desc" to "Amount ↓",
                "amount_asc" to "Amount ↑"
            )
            sortOptions.forEach { (key, label) ->
                FilterChip(
                    selected = sortOption == key,
                    onClick = { sortOption = key },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF254B62),
                        selectedLabelColor = Color.White,
                        labelColor = TxMuted
                    )
                )
            }
        }

        Text(
            "${transactions.size} transactions found",
            color = TxMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (transactions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TxCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No transactions found", color = TxText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (searchQuery.isNotBlank() || sideFilter != "ALL")
                            "No orders match your filter criteria."
                        else
                            "Your completed buy and sell orders will appear here once executed.",
                        color = TxMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (searchQuery.isNotBlank() || sideFilter != "ALL" || sortOption != "newest") {
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                sideFilter = "ALL"
                                sortOption = "newest"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TxGreen, contentColor = Color(0xFF062017)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear Filters", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TxCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (tx.side == "BUY") Color(0xFF124B3D) else Color(0xFF542B36),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        tx.side,
                                        color = if (tx.side == "BUY") TxGreen else TxRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    txMoney(tx.total),
                                    color = TxText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    tx.symbol,
                                    color = TxText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (tx.side == "SELL") {
                                    Text(
                                        "Realized P&L: ${if (tx.realizedPnl >= 0) "+" else ""}${txMoney(tx.realizedPnl)}",
                                        color = if (tx.realizedPnl >= 0) TxGreen else TxRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                "${tx.quantity} shares × ${txMoney(tx.price)}",
                                color = TxMuted,
                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                formatTimestamp(tx.createdAt),
                                color = TxMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
