package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MarketDark = Color(0xFF0F172A)
private val MarketGrey = Color(0xFF64748B)

private data class Stock(
    val symbol: String,
    val name: String,
    val price: String,
    val change: String? = null,
    val isPositive: Boolean? = null,
    val status: MarketDataStatus = MarketDataStatus.DEMO
)

private val sampleStocks = listOf(
    Stock("RELIANCE", "Reliance Industries", "₹1,450.25", "+1.32%", true),
    Stock("TCS", "Tata Consultancy Services", "₹3,920.50", "+0.85%", true),
    Stock("INFY", "Infosys Limited", "₹1,620.30", "+0.60%", true),
    Stock("HDFCBANK", "HDFC Bank", "₹1,780.90", "-0.42%", false),
    Stock("ICICIBANK", "ICICI Bank", "₹1,245.60", "+1.15%", true),
    Stock("ITC", "ITC Limited", "₹420.80", "-0.25%", false),
    Stock("SBIN", "State Bank of India", "₹725.60", "+0.90%", true),
    Stock("WIPRO", "Wipro Limited", "₹290.40", "-0.35%", false)
)

@Composable
fun MarketWatchScreen(
    onStockSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var refreshCount by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var stocksList by remember { mutableStateOf(sampleStocks) }

    val lastUpdated = remember(refreshCount) { MarketFormatters.formatRelativeTime(null) }

    // Manual refresh action updates stock quotes from the market repository
    LaunchedEffect(refreshCount) {
        if (refreshCount > 0) {
            isRefreshing = true
            try {
                val liveQuotes = MarketDataManager.repository.getQuotes()
                if (liveQuotes.isNotEmpty()) {
                    stocksList = liveQuotes.map { quote ->
                        Stock(
                            symbol = quote.symbol,
                            name = quote.companyName,
                            price = MarketFormatters.formatInrPrice(quote.price),
                            change = quote.changePercent?.let { MarketFormatters.formatPercentChange(it) },
                            isPositive = quote.change?.let { it >= 0.0 },
                            status = MarketDataStatus.fromString(quote.status)
                        )
                    }
                }
            } catch (_: Exception) {
                // Preserve current displayed quotes on network error
            } finally {
                isRefreshing = false
            }
        }
    }

    val categories = listOf(
        "All",
        "Nifty 50",
        "Top Gainers",
        "Top Losers"
    )

    // Filter and stably sort stocks deterministically by ticker symbol
    val filteredStocks = stocksList.filter { stock ->
        val matchesSearch =
            stock.symbol.contains(searchQuery, ignoreCase = true) ||
                    stock.name.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (selectedCategory) {
            "Top Gainers" -> stock.isPositive == true
            "Top Losers" -> stock.isPositive == false
            else -> true
        }

        matchesSearch && matchesCategory
    }.sortedBy { it.symbol }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                fontSize = 36.sp,
                color = MarketDark,
                modifier = Modifier
                    .semantics { contentDescription = "Navigate back" }
                    .clickable { onBack() }
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Market Watch",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = MarketDark
                )

                Text(
                    text = "Explore the stock market · $lastUpdated",
                    fontSize = 13.sp,
                    color = MarketGrey
                )
            }

            // Manual refresh action button with progress indicator
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                        .semantics { contentDescription = "Refreshing stock quotes" },
                    color = MarketColors.GainGreen,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = { refreshCount++ },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Refresh stock quotes" }
                ) {
                    Text(
                        text = "↻",
                        fontSize = 22.sp,
                        color = MarketDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text = "📈",
                fontSize = 26.sp,
                modifier = Modifier.semantics { contentDescription = "Market trend indicator" }
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search stocks by name or ticker" },
            placeholder = {
                Text("Search stocks, e.g. Reliance, TCS...")
            },
            leadingIcon = {
                Text("⌕", fontSize = 24.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MarketColors.GainGreen,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "MARKET CATEGORIES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MarketGrey
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Filter category $category" }
                        .clickable { selectedCategory = category },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MarketColors.GainGreen else Color.White
                ) {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = 2.dp,
                            vertical = 11.dp
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (category) {
                                "Nifty 50" -> "Nifty"
                                "Top Gainers" -> "Gainers"
                                "Top Losers" -> "Losers"
                                else -> "All"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) {
                                Color.White
                            } else {
                                MarketDark
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stocks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MarketDark
            )

            Text(
                text = "${filteredStocks.size} stocks",
                fontSize = 12.sp,
                color = MarketGrey
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "COMPANY",
                fontSize = 11.sp,
                color = MarketGrey,
                fontWeight = FontWeight.Bold
            )

            Text(
                "PRICE / CHANGE",
                fontSize = 11.sp,
                color = MarketGrey,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(
                items = filteredStocks,
                key = { it.symbol }
            ) { stock ->
                val stockAccessibilityDesc = "${stock.symbol}, ${stock.name}, Price ${stock.price}, Change ${stock.change}"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = stockAccessibilityDesc }
                        .clickable {
                            onStockSelected(stock.symbol)
                        },
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MarketColors.GainGreenLight
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stock.symbol.take(1),
                                    color = MarketColors.GainGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stock.symbol,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MarketDark
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = stock.name,
                                fontSize = 11.sp,
                                color = MarketGrey,
                                maxLines = 1
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stock.price,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MarketDark
                            )

                            Spacer(Modifier.height(4.dp))

                            if (stock.change != null && stock.isPositive != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MarketColors.changeBackground(stock.isPositive)
                                ) {
                                    Text(
                                        text = stock.change,
                                        modifier = Modifier.padding(
                                            horizontal = 7.dp,
                                            vertical = 4.dp
                                        ),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MarketColors.changeColor(stock.isPositive)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredStocks.isEmpty()) {
                item {
                    Text(
                        text = "No stocks found. Try another search.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        color = MarketGrey
                    )
                }
            }
        }

        val overallStatus = stocksList.firstOrNull()?.status ?: MarketDataStatus.DEMO
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = overallStatus.badgeColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = overallStatus.noticeText(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = overallStatus.textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
