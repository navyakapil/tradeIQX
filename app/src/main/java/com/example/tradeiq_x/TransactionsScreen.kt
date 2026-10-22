
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val TransactionNavy = Color(0xFF0F172A)
private val TransactionGreen = Color(0xFF16A34A)
private val TransactionRed = Color(0xFFEF4444)
private val TransactionBg = Color(0xFFF8FAFC)
private val TransactionGray = Color(0xFF64748B)

private data class DemoTransaction(
    val id: Int,
    val type: String,
    val symbol: String,
    val quantity: Int,
    val price: Double,
    val date: String
)

private val demoTransactions = listOf(
    DemoTransaction(1, "BUY", "RELIANCE", 10, 1450.0, "24 Sep 2026"),
    DemoTransaction(2, "SELL", "TCS", 2, 3200.0, "23 Sep 2026"),
    DemoTransaction(3, "BUY", "INFY", 8, 1600.0, "22 Sep 2026"),
    DemoTransaction(4, "SELL", "ITC", 10, 420.0, "21 Sep 2026"),
    DemoTransaction(5, "BUY", "HDFCBANK", 3, 1780.0, "20 Sep 2026"),
    DemoTransaction(6, "BUY", "TCS", 7, 3200.0, "19 Sep 2026"),
    DemoTransaction(7, "BUY", "ITC", 20, 420.0, "18 Sep 2026"),
    DemoTransaction(8, "SELL", "SBIN", 5, 735.0, "17 Sep 2026"),
    DemoTransaction(9, "BUY", "WIPRO", 15, 480.0, "16 Sep 2026"),
    DemoTransaction(10, "BUY", "AXISBANK", 6, 1100.0, "15 Sep 2026"),
    DemoTransaction(11, "BUY", "SBIN", 17, 725.0, "14 Sep 2026"),
    DemoTransaction(12, "SELL", "RELIANCE", 3, 1480.0, "13 Sep 2026")
)

private fun transactionMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(
        Locale("en", "IN")
    )
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "₹${formatter.format(amount)}"
}

@Composable
fun TransactionsScreen(
    onBackClick: () -> Unit = {}
) {
    var selectedFilter by rememberSaveable {
        mutableStateOf("ALL")
    }

    val filteredTransactions = demoTransactions.filter {
        selectedFilter == "ALL" || it.type == selectedFilter
    }

    BackHandler(onBack = onBackClick)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TransactionBg)
    ) {

        // FIXED HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TransactionNavy)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text(
                    "← Back",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                "Transaction History",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // FILTER BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "BUY", "SELL").forEach { filter ->
                Button(
                    onClick = {
                        selectedFilter = filter
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedFilter == filter)
                            TransactionNavy
                        else
                            Color(0xFFE2E8F0),
                        contentColor = if (selectedFilter == filter)
                            Color.White
                        else
                            TransactionNavy
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        filter,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            "${filteredTransactions.size} Transactions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TransactionNavy,
            modifier = Modifier.padding(
                start = 16.dp,
                bottom = 12.dp
            )
        )

        // VERTICALLY SCROLLABLE LIST
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = true,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = filteredTransactions,
                key = { it.id }
            ) { transaction ->
                DemoTransactionCard(transaction)
            }
        }

        // FIXED BOTTOM BUTTON
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TransactionGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "← Back to Portfolio",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DemoTransactionCard(
    transaction: DemoTransaction
) {
    val typeColor = if (transaction.type == "BUY")
        TransactionGreen
    else
        TransactionRed

    val totalAmount = transaction.quantity * transaction.price

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    transaction.type,
                    color = typeColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    transactionMoney(totalAmount),
                    color = TransactionNavy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                transaction.symbol,
                color = TransactionNavy,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "${transaction.quantity} shares @ " +
                        transactionMoney(transaction.price),
                color = TransactionGray,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                transaction.date,
                color = TransactionGray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "● Completed",
                color = TransactionGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
