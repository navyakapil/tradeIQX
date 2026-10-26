package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException

private val DetailsBg = Color(0xFF071522)
private val DetailsCard = Color(0xFF132838)
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
    val fallbackPrice = TradingStore.prices[stock] ?: 0.0

    var selectedRange by remember { mutableStateOf(ChartRange.ONE_DAY) }
    var currentQuote by remember { mutableStateOf<StockQuote?>(null) }
    var candles by remember { mutableStateOf<List<PriceCandle>>(emptyList()) }
    var isMarketLoading by remember { mutableStateOf(false) }
    var chartError by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    // Automatically cancels previous in-flight coroutine if stock or range changes
    LaunchedEffect(stock, selectedRange, reloadTrigger) {
        isMarketLoading = true
        chartError = null
        try {
            val fetchedQuote = MarketDataManager.repository.getQuote(stock)
            val fetchedCandles = MarketDataManager.repository.getHistory(stock, selectedRange)
            currentQuote = fetchedQuote
            candles = fetchedCandles
            if (fetchedCandles.isEmpty()) {
                chartError = "Historical chart data unavailable for $stock"
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            chartError = "Failed to load chart data. Please try again."
        } finally {
            isMarketLoading = false
        }
    }

    val price = currentQuote?.price ?: fallbackPrice
    val lastUpdated = MarketFormatters.formatRelativeTime(currentQuote?.lastUpdated)

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
                        color = MarketColors.darkChangeColor(sampleChange >= 0),
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

                // Chart container with loading, empty/error, and candle visualizer states
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isMarketLoading -> {
                            CircularProgressIndicator(
                                color = MarketColors.DarkGainGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        chartError != null || candles.isEmpty() -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = chartError ?: "Chart data unavailable for $stock",
                                    color = DetailsMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(
                                    onClick = { reloadTrigger++ },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MarketColors.DarkGainGreen
                                    )
                                ) {
                                    Text(
                                        "Retry",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        else -> {
                            val minPrice = candles.minOfOrNull { it.low } ?: 0.0
                            val maxPrice = candles.maxOfOrNull { it.high } ?: 1.0
                            val priceRange = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                candles.forEach { candle ->
                                    val barHeight = (((candle.close - minPrice) / priceRange) * 75 + 15)
                                        .coerceIn(10.0, 95.0)
                                    val isBarGain = candle.close >= candle.open

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(barHeight.dp)
                                            .background(
                                                MarketColors.darkChangeColor(isBarGain),
                                                RoundedCornerShape(
                                                    topStart = 3.dp,
                                                    topEnd = 3.dp
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Illustrative chart · ${MarketConstants.NOTICE_DEMO_DATA}",
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
                    containerColor = MarketColors.DarkGainGreen,
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
                    containerColor = MarketColors.DarkLossRed,
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
            text = "Practice trading with virtual money · ${MarketConstants.NOTICE_DEMO_DATA}",
            color = DetailsMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
