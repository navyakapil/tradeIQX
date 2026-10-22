
package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BuyBackground = Color(0xFFF5F7FC)
private val BuyDark = Color(0xFF17233C)
private val BuyGrey = Color(0xFF64748B)
private val BuyGreen = Color(0xFF16A34A)
private val BuyRed = Color(0xFFDC2626)

@Composable
fun BuyScreen(
    onBack: () -> Unit,
    onSell: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BuyBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back")
        }

        Text(
            text = "Buy RELIANCE",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BuyDark
        )

        Spacer(Modifier.height(16.dp))

        Text("Share price: ₹1,450.25", color = BuyGrey)
        Text("Virtual balance: ₹10,00,000", color = BuyGreen)

        Spacer(Modifier.height(20.dp))

        Text("Buy screen — quantity and calculations will be added next.")

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onSell,
            colors = ButtonDefaults.buttonColors(
                containerColor = BuyRed
            )
        ) {
            Text("Go to Sell")
        }
    }
}
