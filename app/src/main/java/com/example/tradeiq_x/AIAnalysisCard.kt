
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

data class StockAnalysis(
    val signal: String,
    val risk: String,
    val explanation: String
)

fun analyzeStock(symbol: String): StockAnalysis {
    return when (symbol.uppercase()) {
        "RELIANCE" -> StockAnalysis(
            signal = "HOLD",
            risk = "Medium",
            explanation = "Sample neutral outlook for this stock."
        )

        "TCS" -> StockAnalysis(
            signal = "BUY",
            risk = "Low",
            explanation = "Sample positive outlook for this stock."
        )

        "INFY" -> StockAnalysis(
            signal = "SELL",
            risk = "High",
            explanation = "Sample negative outlook for this stock."
        )

        "HDFCBANK" -> StockAnalysis(
            signal = "HOLD",
            risk = "Medium",
            explanation = "Sample mixed outlook for this stock."
        )

        else -> StockAnalysis(
            signal = "HOLD",
            risk = "Unknown",
            explanation = "No sample analysis is available."
        )
    }
}

@Composable
fun AIAnalysisCard(symbol: String) {
    val analysis = analyzeStock(symbol)

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
                text = "Fixed sample data, not a real AI prediction or investment advice.",
                color = Color(0xFF9FB0C0),
                fontSize = 12.sp
            )
        }
    }
}
