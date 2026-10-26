
package com.example.tradeiq_x

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.CancellationException

@Composable
fun AIAnalysisCard(symbol: String) {

    // Local simulated stock analysis
    val analysis = remember(symbol) {
        StockAnalysisEngine.analyze(symbol)
    }

    var backendExplanation by remember(symbol) {
        mutableStateOf("")
    }

    var isLoading by remember(symbol) {
        mutableStateOf(true)
    }

    var backendConnected by remember(symbol) {
        mutableStateOf(false)
    }

    // Request explanation from FastAPI
    LaunchedEffect(symbol) {

        isLoading = true
        backendConnected = false
        backendExplanation = ""

        try {
            backendExplanation = AIService.getStockExplanation(
                symbol = symbol,
                analysis = analysis
            )

            backendConnected = true

            Log.d(
                "TradeIQX_AI",
                "Backend analysis connected successfully"
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(
                "TradeIQX_AI",
                "Backend analysis request failed",
                e
            )

            backendExplanation =
                "Connection error: ${e.message ?: "Unknown error"}"

            backendConnected = false

        } finally {
            isLoading = false
        }
    }

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

            HorizontalDivider(
                color = Color(0xFF355063)
            )

            Text(
                text = "Backend Analysis Explanation",
                color = Color(0xFF32D49A),
                fontWeight = FontWeight.Bold
            )

            if (isLoading) {

                CircularProgressIndicator(
                    color = Color(0xFF32D49A),
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "Connecting to backend...",
                    color = Color.White,
                    fontSize = 14.sp
                )

            } else {

                Text(
                    text = backendExplanation,
                    color = if (backendConnected) {
                        Color.White
                    } else {
                        Color(0xFFFFD166)
                    },
                    fontSize = 14.sp
                )

                Text(
                    text = if (backendConnected) {
                        "Backend connected (template explanation)"
                    } else {
                        "Offline: backend request failed"
                    },
                    color = if (backendConnected) {
                        Color(0xFF32D49A)
                    } else {
                        Color(0xFFFFD166)
                    },
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
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
