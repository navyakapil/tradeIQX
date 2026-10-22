
package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SellBackground = Color(0xFFF5F7FC)
private val SellDark = Color(0xFF17233C)
private val SellGrey = Color(0xFF64748B)
private val SellGreen = Color(0xFF16A34A)

@Composable
fun SellScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SellBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back")
        }

        Text(
            text = "Sell RELIANCE",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = SellDark
        )

        Spacer(Modifier.height(16.dp))

        Text("Shares owned: 10", color = SellGrey)
        Text("Share price: ₹1,450.25", color = SellGreen)

        Spacer(Modifier.height(20.dp))

        Text("Sell screen — quantity and calculations will be added next.")
    }
}
