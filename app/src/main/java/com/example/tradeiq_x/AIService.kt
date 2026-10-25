
package com.example.tradeiq_x

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIService {

    // Android emulator connects to laptop FastAPI using 10.0.2.2
    private const val BASE_URL = "http://127.0.0.1:8001"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Send JSON requests to FastAPI
    private suspend fun postJson(
        endpoint: String,
        json: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {

        val body = json.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url("$BASE_URL$endpoint")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->

                val responseText = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    throw IOException(
                        "Server error ${response.code}: $responseText"
                    )
                }

                if (responseText.isBlank()) {
                    throw IOException(
                        "The server returned an empty response."
                    )
                }

                JSONObject(responseText)
            }
        } catch (e: IOException) {
            throw IOException(
                "Cannot connect to TradeIQX backend at $BASE_URL. " +
                        "Check that FastAPI is running on port 8001. " +
                        "Details: ${e.message}",
                e
            )
        }
    }

    // Stock analysis explanation
    suspend fun getStockExplanation(
        symbol: String,
        analysis: AnalysisResult
    ): String {

        val requestJson = JSONObject().apply {
            put("symbol", symbol.uppercase())
            put("signal", analysis.signal)
            put("risk", analysis.risk)
            put("indicators", analysis.explanation)
        }

        val result = postJson("/analyze", requestJson)

        return result.optString(
            "explanation",
            "No stock analysis explanation was returned."
        )
    }

    // RAG-based AI Trading Mentor
    suspend fun askMentor(question: String): String {

        require(question.isNotBlank()) {
            "Please enter a question."
        }

        val requestJson = JSONObject().apply {
            put("message", question.trim())
        }

        val result = postJson("/api/chat", requestJson)

        val answer = result.optString("reply", "").trim()

        if (answer.isBlank()) {
            throw IOException(
                "The AI Mentor returned an empty answer."
            )
        }

        return answer
    }
}
