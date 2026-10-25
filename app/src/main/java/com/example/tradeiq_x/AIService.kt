


package com.example.tradeiq_x

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object AIService {

    private val client = OkHttpClient()

    // Android Emulator connects to your computer using 10.0.2.2.
    // This endpoint will work only after we create and run the backend.
    private const val BASE_URL = "http://10.0.2.2:8000"

    suspend fun getStockExplanation(
        symbol: String,
        analysis: AnalysisResult
    ): String = withContext(Dispatchers.IO) {

        val requestJson = JSONObject().apply {
            put("symbol", symbol.uppercase())
            put("signal", analysis.signal)
            put("risk", analysis.risk)
            put("indicators", analysis.explanation)
        }

        val body = requestJson.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/analyze")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Backend error: ${response.code}")
            }

            val responseText = response.body?.string()
                ?: throw IOException("Empty backend response")

            JSONObject(responseText).getString("explanation")
        }
    }
}
