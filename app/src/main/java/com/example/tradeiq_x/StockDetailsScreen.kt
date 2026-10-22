package com.example.tradeiq_x


import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



private val StockBackground = Color(0xFFF8FAFC)
private val StockDark = Color(0xFF0F172A)
private val StockGreen = Color(0xFF16A34A)
private val StockRed = Color(0xFFEF4444)
private val StockGrey = Color(0xFF64748B)

@Composable
fun StockDetailsScreen(
    onBuy: () -> Unit,
    onSell: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back")
        }

        Text(
            "Stock Details",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = StockDark
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "RELIANCE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Reliance Industries Ltd.",
                    color = StockGrey
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "₹1,450.25",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockDark
                )
                Text("+18.90 (+1.32%)", color = StockGreen)

                Spacer(Modifier.height(20.dp))

                Text("Today's price movement", color = StockGrey)
                Text(
                    "▁▂▃▂▄▅▄▆▅▇▆█",
                    fontSize = 26.sp,
                    color = StockGreen
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBuy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StockGreen
                )
            ) {
                Text("Buy")
            }

            Button(
                onClick = onSell,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StockRed
                )
            ) {
                Text("Sell")
            }
        }
    }
}
