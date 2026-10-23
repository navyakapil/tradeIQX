
package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PortfolioNavy = Color(0xFF0F172A)
private val PortfolioGreen = Color(0xFF16A34A)
private val PortfolioRed = Color(0xFFEF4444)
private val PortfolioBg = Color(0xFFF8FAFC)

private data class PortfolioHolding(
    val symbol: String,
    val quantity: Int,
    val averagePrice: Double,
    val currentPrice: Double
)

private val sampleHoldings = listOf(
    PortfolioHolding("RELIANCE", 10, 1450.0, 1480.0),
    PortfolioHolding("TCS", 5, 3200.0, 3150.0),
    PortfolioHolding("INFY", 8, 1600.0, 1625.0),
    PortfolioHolding("HDFCBANK", 3, 1780.0, 1800.0),
    PortfolioHolding("ITC", 10, 420.0, 418.0),
    PortfolioHolding("SBIN", 12, 725.0, 735.0),
    PortfolioHolding("WIPRO", 15, 480.0, 495.0),
    PortfolioHolding("AXISBANK", 6, 1100.0, 1125.0)
)

@Composable
fun PortfolioScreen(
    onTransactionsClick: () -> Unit = {}
) {
    val invested = sampleHoldings.sumOf {
        it.quantity * it.averagePrice
    }

    val current = sampleHoldings.sumOf {
        it.quantity * it.currentPrice
    }

    val profit = current - invested
    val profitColor =
        if (profit >= 0) PortfolioGreen else PortfolioRed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortfolioBg)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PortfolioNavy)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "My Portfolio",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // VERTICALLY SCROLLABLE CONTENT
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = true,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0FDF4)
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Total Portfolio Value",
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "₹${String.format("%,.2f", current)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PortfolioNavy
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "P&L: ₹${String.format("%,.2f", profit)}",
                            color = profitColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Text(
                    "Invested Value: ₹${String.format("%,.2f", invested)}",
                    fontWeight = FontWeight.Medium,
                    color = PortfolioNavy
                )
            }

            item {
                Text(
                    "My Holdings (${sampleHoldings.size})",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = PortfolioNavy
                )
            }

            items(sampleHoldings) { stock ->
                val stockProfit =
                    stock.quantity *
                            (stock.currentPrice - stock.averagePrice)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stock.symbol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PortfolioNavy
                        )

                        Spacer(Modifier.height(8.dp))

                        Text("Quantity: ${stock.quantity}")
                        Text("Average Price: ₹${stock.averagePrice}")
                        Text("Current Price: ₹${stock.currentPrice}")

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "P&L: ₹${String.format("%,.2f", stockProfit)}",
                            color = if (stockProfit >= 0)
                                PortfolioGreen else PortfolioRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // FIXED BUTTON
        Button(
            onClick = onTransactionsClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PortfolioGreen
            )
        ) {
            Text("View Transaction History →")
        }
    }
}
