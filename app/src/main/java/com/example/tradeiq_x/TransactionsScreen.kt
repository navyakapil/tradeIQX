package com.example.tradeiq_x

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private val TxBg = Color(0xFF071522)
private val TxCard = Color(0xFF132838)
private val TxGreen = Color(0xFF32D49A)
private val TxRed = Color(0xFFFF5364)
private val TxText = Color(0xFFF8FAFC)
private val TxMuted = Color(0xFF9FB0C0)
private val OfflineBannerBg = Color(0xFF2A1F0D)
private val OfflineText = Color(0xFFFFB74D)

private fun txMoney(value: Double): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

@Composable
fun TransactionsScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        TradingStore.init(context)
        TradingStore.refresh(context)
    }

    var filter by rememberSaveable { mutableStateOf("ALL") }
    var sortBy by rememberSaveable { mutableStateOf("NEWEST") }

    val rawTxs = TradingStore.transactions.toList()
    val filtered = rawTxs.filter { filter == "ALL" || it.type == filter }
    val txs = when (sortBy) {
        "OLDEST" -> filtered.sortedBy { it.timestamp }
        "AMOUNT" -> filtered.sortedByDescending { it.total }
        else -> filtered.sortedByDescending { it.timestamp }
    }

    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("en", "IN"))
    }

    BackHandler(onBack = onBackClick)

    Column(
        Modifier
            .fillMaxSize()
            .background(TxBg)
            .padding(16.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text("‹ Back", color = TxText)
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
                    "Transactions",
                    color = TxText,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Your executed paper trades",
                    color = TxMuted,
                    fontSize = 13.sp
                )
            }

            if (TradingStore.isLoading.value) {
                CircularProgressIndicator(
                    color = TxGreen,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Direction Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "BUY", "SELL").forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TxGreen,
                        selectedLabelColor = Color(0xFF062017),
                        labelColor = TxText
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Sort order chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("NEWEST" to "Newest", "OLDEST" to "Oldest", "AMOUNT" to "Amount").forEach { (key, label) ->
                FilterChip(
                    selected = sortBy == key,
                    onClick = { sortBy = key },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E3A5F),
                        selectedLabelColor = TxText,
                        labelColor = TxMuted
                    )
                )
            }
        }

        Text(
            "${txs.size} transactions",
            color = TxMuted,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        if (txs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TxCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        "No transactions yet",
                        color = TxText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your completed buy and sell orders will appear here.",
                        color = TxMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(txs, key = { it.id }) { tx ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TxCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (tx.type == "BUY")
                                        Color(0xFF124B3D)
                                    else
                                        Color(0xFF542B36),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        tx.type,
                                        color = if (tx.type == "BUY") TxGreen else TxRed,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 5.dp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    txMoney(tx.total),
                                    color = TxText,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                tx.symbol,
                                color = TxText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "${tx.quantity} shares @ ${txMoney(tx.price)}",
                                color = TxMuted,
                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                formatter.format(Date(tx.timestamp)),
                                color = TxMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
