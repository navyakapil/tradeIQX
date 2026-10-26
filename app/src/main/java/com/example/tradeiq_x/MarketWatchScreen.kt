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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MarketGreen = Color(0xFF16A34A)
private val MarketDark = Color(0xFF0F172A)
private val MarketGrey = Color(0xFF64748B)

private data class Stock(
    val symbol: String,
    val name: String,
    val price: String,
    val change: String,
    val isPositive: Boolean
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

    val categories = listOf(
        "All",
        "Nifty 50",
        "Top Gainers",
        "Top Losers"
    )

    val filteredStocks = sampleStocks.filter { stock ->
        val matchesSearch =
            stock.symbol.contains(searchQuery, ignoreCase = true) ||
                    stock.name.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (selectedCategory) {
            "Top Gainers" -> stock.isPositive
            "Top Losers" -> !stock.isPositive
            else -> true
        }

        matchesSearch && matchesCategory
    }

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
                modifier = Modifier.clickable { onBack() }
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
                    text = "Explore the stock market",
                    fontSize = 13.sp,
                    color = MarketGrey
                )
            }

            Text(
                text = "📈",
                fontSize = 26.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search stocks, e.g. Reliance, TCS...")
            },
            leadingIcon = {
                Text("⌕", fontSize = 24.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MarketGreen,
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
                        .clickable { selectedCategory = category },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MarketGreen else Color.White
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
            items(filteredStocks) { stock ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            color = Color(0xFFDCFCE7)
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stock.symbol.take(1),
                                    color = MarketGreen,
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

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (stock.isPositive) {
                                    Color(0xFFDCFCE7)
                                } else {
                                    Color(0xFFFEE2E2)
                                }
                            ) {
                                Text(
                                    text = stock.change,
                                    modifier = Modifier.padding(
                                        horizontal = 7.dp,
                                        vertical = 4.dp
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (stock.isPositive) {
                                        MarketGreen
                                    } else {
                                        Color(0xFFDC2626)
                                    }
                                )
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

        Text(
            text = "Demo prices • Not live market data",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            fontSize = 11.sp,
            color = MarketGrey
        )
    }
}
