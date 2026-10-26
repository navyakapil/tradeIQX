package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DetailsBg = Color(0xFF071522)
private val DetailsCard = Color(0xFF132838)
private val DetailsGreen = Color(0xFF32D49A)
private val DetailsRed = Color(0xFFFF5364)
private val DetailsWhite = Color(0xFFF8FAFC)
private val DetailsMuted = Color(0xFF9FB0C0)

@Composable
fun StockDetailsScreen(
    symbol: String,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    onBack: () -> Unit
) {
    val stock = symbol.uppercase()
    val price = TradingStore.prices[stock] ?: 0.0
    val lastUpdated = remember { MarketFormatters.formatRelativeTime(null) }

    val companyNames = mapOf(
        "RELIANCE" to "Reliance Industries Ltd.",
        "TCS" to "Tata Consultancy Services",
        "INFY" to "Infosys Limited",
        "HDFCBANK" to "HDFC Bank Limited"
    )

    val company = companyNames[stock] ?: stock

    // Illustrative movement only, not real market performance.
    val sampleChange = when (stock) {
        "TCS" -> 1.20
        "INFY" -> 0.60
        "HDFCBANK" -> -0.42
        else -> 1.32
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailsBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Back navigation
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
            colors = ButtonDefaults.textButtonColors(
                contentColor = DetailsWhite
            )
        ) {
            Text("‹  Back", fontSize = 15.sp)
        }

        Text(
            text = "Stock Details",
            color = DetailsWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        // Main stock card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = DetailsCard
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stock,
                    color = DetailsWhite,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = company,
                    color = DetailsMuted,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = MarketFormatters.formatInrPrice(price),
                    color = DetailsWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = MarketFormatters.formatPercentChange(sampleChange),
                        color = if (sampleChange >= 0)
                            DetailsGreen else DetailsRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "• $lastUpdated",
                        color = DetailsMuted,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Price movement",
                    color = DetailsMuted,
                    fontSize = 14.sp
                )

                // Decorative chart bars; not historical market data.
                val bars = listOf(
                    12, 18, 14, 25, 21, 32, 27, 38, 31, 45, 39, 53
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    bars.forEachIndexed { index, height ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(height.dp)
                                .background(
                                    if (sampleChange >= 0)
                                        DetailsGreen
                                    else
                                        DetailsRed,
                                    RoundedCornerShape(
                                        topStart = 3.dp,
                                        topEnd = 3.dp
                                    )
                                )
                        )
                    }
                }

                Text(
                    text = "Illustrative chart · Simulated prices",
                    color = DetailsMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Key statistics
        Text(
            text = "Stock overview",
            color = DetailsWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailStatCard(
                title = "Trading price",
                value = MarketFormatters.formatInrPrice(price),
                modifier = Modifier.weight(1f)
            )

            DetailStatCard(
                title = "Your holdings",
                value = "${TradingStore.holdings[stock] ?: 0} shares",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailStatCard(
                title = "Avg. buy price",
                value = MarketFormatters.formatInrPrice(
                    TradingStore.averagePrices[stock] ?: 0.0
                ),
                modifier = Modifier.weight(1f)
            )

            DetailStatCard(
                title = "Virtual balance",
                value = MarketFormatters.formatInrPrice(TradingStore.balance.value),
                modifier = Modifier.weight(1f)
            )
        }
        // AI Analysis Section
        AIAnalysisCard(symbol = stock)

        // Trading actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBuy,
                enabled = price > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DetailsGreen,
                    contentColor = Color(0xFF062017)
                )
            ) {
                Text(
                    "Buy",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onSell,
                enabled = price > 0 &&
                        (TradingStore.holdings[stock] ?: 0) > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DetailsRed,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Sell",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Practice trading with virtual money. " +
                    "Displayed prices and chart movements are simulated.",
            color = DetailsMuted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DetailStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DetailsCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = DetailsMuted,
                fontSize = 12.sp
            )

            Text(
                text = value,
                color = DetailsWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
