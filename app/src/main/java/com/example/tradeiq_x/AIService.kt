

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

    // Android Emulator -> FastAPI running on Windows
    private val BASE_URL: String
        get() = if (BuildConfig.DEBUG || BuildConfig.BACKEND_BASE_URL.isBlank()) {
            "http://127.0.0.1:8002"
        } else {
            BackendApi.baseUrl
        }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // ---------------------------------
    // SEND JSON REQUESTS TO FASTAPI
    // ---------------------------------

    private suspend fun postJson(
        endpoint: String,
        json: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {

        val body = json.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )
        android.util.Log.d("TRADEIQX_AI", "Calling: $BASE_URL$endpoint")
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
                "Cannot connect to TradeIQX AI backend at $BASE_URL. " +
                        "Check that FastAPI is running on port 8002. " +
                        "Details: ${e.message}",
                e
            )
        }
    }

    // ---------------------------------
    // STOCK ANALYSIS EXPLANATION
    // ---------------------------------

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

    // ---------------------------------
    // RAG-BASED AI TRADING MENTOR
    // ---------------------------------

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

    // ---------------------------------
    // AI-POWERED FINANCIAL QUIZ
    // ---------------------------------

    suspend fun generateQuiz(
        topic: String = "Stock Market Basics",
        difficulty: String = "Beginner"
    ): JSONObject {

        require(topic.isNotBlank()) {
            "Please select a quiz topic."
        }

        require(
            difficulty in listOf(
                "Beginner",
                "Intermediate",
                "Advanced"
            )
        ) {
            "Invalid quiz difficulty."
        }

        val requestJson = JSONObject().apply {
            put("topic", topic.trim())
            put("difficulty", difficulty)
        }

        val result = postJson(
            "/api/quiz/generate",
            requestJson
        )

        val questions = result.optJSONArray("questions")

        if (questions == null || questions.length() == 0) {
            throw IOException(
                "The AI did not generate any quiz questions."
            )
        }

        return result
    }
}
