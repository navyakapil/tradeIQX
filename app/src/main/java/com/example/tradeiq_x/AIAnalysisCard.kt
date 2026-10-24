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
                text = "Analysis: HOLD (Demo)",
                color = Color(0xFF32D49A),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "This is a sample educational insight. " +
                        "A trained AI model has not yet analyzed this stock.",
                color = Color.White,
                fontSize = 14.sp
            )

            Text(
                text = "Demo only · Not investment advice",
                color = Color(0xFF9FB0C0),
                fontSize = 12.sp
            )
        }
    }
}
