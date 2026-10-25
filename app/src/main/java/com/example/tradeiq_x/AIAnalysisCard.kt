

package com.example.tradeiq_x

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AIAnalysisCard(symbol: String) {

    // Get calculated results from StockAnalysisEngine.kt
    val analysis = StockAnalysisEngine.analyze(symbol)

    val signalColor = when (analysis.signal) {
        "BUY" -> Color(0xFF32D49A)
        "SELL" -> Color(0xFFFF6B6B)
        else -> Color(0xFFFFD166)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF132838)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "AI Stock Analysis",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Stock: ${symbol.uppercase()}",
                color = Color(0xFF9FB0C0)
            )

            Text(
                text = "Simulated Signal: ${analysis.signal}",
                color = signalColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Risk Indicator: ${analysis.risk} (Demo)",
                color = Color.White
            )

            Text(
                text = analysis.explanation,
                color = Color.White,
                fontSize = 14.sp
            )

            Text(
                text = "Calculated from sample historical prices. " +
                        "Not a real AI prediction or investment advice.",
                color = Color(0xFF9FB0C0),
                fontSize = 12.sp
            )
        }
    }
}
