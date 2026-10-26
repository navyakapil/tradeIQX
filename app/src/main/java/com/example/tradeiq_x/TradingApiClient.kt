package com.example.tradeiq_x

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TradingApiClient {
    const val BASE_URL: String = "http://10.0.2.2:8000"
    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 10000

    private fun extractErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val detail = json.opt("detail")
            when (detail) {
                is String -> detail
                is JSONArray -> {
                    val messages = mutableListOf<String>()
                    for (i in 0 until detail.length()) {
                        val item = detail.optJSONObject(i)
                        val msg = item?.optString("msg") ?: detail.optString(i)
                        if (msg.isNotBlank()) messages.add(msg)
                    }
                    if (messages.isNotEmpty()) messages.joinToString("; ") else "Validation error ($statusCode)"
                }
                else -> defaultErrorMessage(statusCode)
            }
        } catch (_: Exception) {
            defaultErrorMessage(statusCode)
        }
    }

    private fun defaultErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            400 -> "Bad request. Please check your order details."
            404 -> "Trading resource not found."
            422 -> "Validation error. Please verify trade inputs."
            500 -> "Trading server error. Please try again later."
            503 -> "Trading service is temporarily unavailable."
            else -> "Request failed with status $statusCode"
        }
    }

    private suspend fun get(endpoint: String, userId: String): String = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL$endpoint")
        val connection = try {
            url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw TradingError(0, "Unable to connect to trading server: ${e.localizedMessage ?: "Connection failed"}")
        }

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-User-Id", userId)

            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                throw TradingError(0, "Unable to reach trading server at $BASE_URL")
            }

            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

            if (status !in 200..299) {
                throw TradingError(status, extractErrorMessage(response, status))
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun post(endpoint: String, payload: JSONObject, userId: String): String = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL$endpoint")
        val connection = try {
            url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw TradingError(0, "Unable to connect to trading server: ${e.localizedMessage ?: "Connection failed"}")
        }

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-User-Id", userId)

            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                throw TradingError(0, "Unable to reach trading server at $BASE_URL")
            }

            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

            if (status !in 200..299) {
                throw TradingError(status, extractErrorMessage(response, status))
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getQuote(symbol: String, userId: String = "default_user"): JSONObject {
        val safeSymbol = URLEncoder.encode(symbol.trim().uppercase(), "UTF-8")
        val raw = get("/quote/$safeSymbol", userId)
        return JSONObject(raw)
    }

    suspend fun submitOrder(
        symbol: String,
        side: String,
        quantity: Int,
        userId: String = "default_user",
        idempotencyKey: String? = null
    ): JSONObject {
        val payload = JSONObject().apply {
            put("symbol", symbol.trim().uppercase())
            put("side", side.trim().uppercase())
            put("quantity", quantity)
            if (!idempotencyKey.isNullOrBlank()) {
                put("idempotency_key", idempotencyKey)
            }
        }
        val raw = post("/orders", payload, userId)
        return JSONObject(raw)
    }

    suspend fun getPortfolio(userId: String = "default_user"): JSONObject {
        val raw = get("/portfolio", userId)
        return JSONObject(raw)
    }

    suspend fun getTransactions(
        userId: String = "default_user",
        symbol: String? = null,
        side: String? = null,
        sort: String? = "newest",
        limit: Int = 50,
        offset: Int = 0
    ): JSONArray {
        val params = mutableListOf<String>()
        if (!symbol.isNullOrBlank()) params.add("symbol=${URLEncoder.encode(symbol.trim().uppercase(), "UTF-8")}")
        if (!side.isNullOrBlank() && side != "ALL") params.add("side=${URLEncoder.encode(side.trim().uppercase(), "UTF-8")}")
        if (!sort.isNullOrBlank()) params.add("sort=${URLEncoder.encode(sort.trim(), "UTF-8")}")
        params.add("limit=$limit")
        params.add("offset=$offset")

        val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        val raw = get("/transactions$query", userId)
        return JSONArray(raw)
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }
}
