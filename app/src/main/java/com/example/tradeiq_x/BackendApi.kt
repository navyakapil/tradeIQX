package com.example.tradeiq_x

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class BackendApiException(val statusCode: Int, message: String) : Exception(message)

object BackendApi {
    @Volatile
    var baseUrl: String = if (BuildConfig.DEBUG || BuildConfig.BACKEND_BASE_URL.isBlank()) {
        "http://10.0.2.2:8001"
    } else {
        BuildConfig.BACKEND_BASE_URL.trimEnd('/')
    }
        private set

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    private fun extractErrorMessage(response: String, statusCode: Int): String {
        return try {
            val json = JSONObject(response)
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
            400 -> "Bad request. Please verify your input."
            401 -> "Invalid email or password."
            404 -> "Requested resource not found."
            409 -> "An account with this email or username already exists."
            422 -> "Validation failed. Please check the entered fields."
            500 -> "Internal server error. Please try again later."
            503 -> "Backend service is temporarily unavailable."
            else -> "Request failed with status $statusCode"
        }
    }

    private suspend fun post(
        endpoint: String,
        data: JSONObject,
        userId: Int? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = try {
            URL("$baseUrl$endpoint").openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw BackendApiException(0, "Unable to connect to server: ${e.localizedMessage ?: "Connection error"}")
        }

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            if (userId != null) {
                connection.setRequestProperty("X-User-Id", userId.toString())
            }

            try {
                connection.outputStream.use {
                    it.write(data.toString().toByteArray(Charsets.UTF_8))
                }
            } catch (e: IOException) {
                throw BackendApiException(0, "Failed to send data to server: ${e.localizedMessage ?: "Network error"}")
            }

            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                throw BackendApiException(0, "Unable to reach backend server. Please verify backend is running.")
            }

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

            if (status !in 200..299) {
                val message = extractErrorMessage(response, status)
                throw BackendApiException(status, message)
            }

            JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun get(
        endpoint: String,
        userId: Int? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = try {
            URL("$baseUrl$endpoint").openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw BackendApiException(0, "Unable to connect to server: ${e.localizedMessage ?: "Connection error"}")
        }

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            if (userId != null) {
                connection.setRequestProperty("X-User-Id", userId.toString())
            }

            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                throw BackendApiException(0, "Unable to reach backend server. Please verify backend is running.")
            }

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

            if (status !in 200..299) {
                val message = extractErrorMessage(response, status)
                throw BackendApiException(status, message)
            }

            JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun credentials(
        email: String,
        password: String
    ): JSONObject = JSONObject().apply {
        put("email", email.trim().lowercase())
        put("password", password)
    }

    suspend fun signup(
        username: String,
        email: String,
        password: String
    ): JSONObject {
        val data = JSONObject().apply {
            put("username", username.trim())
            put("email", email.trim().lowercase())
            put("password", password)
        }
        return post("/auth/signup", data)
    }

    suspend fun login(
        email: String,
        password: String
    ): JSONObject {
        return post("/auth/login", credentials(email, password))
    }

    suspend fun getProfile(userId: Int? = null): JSONObject {
        val endpoint = if (userId != null) "/auth/me?user_id=$userId" else "/auth/me"
        return get(endpoint, userId)
    }

    suspend fun getLiveQuote(symbol: String): JSONObject =
        withContext(Dispatchers.IO) {
            val safeSymbol = symbol.trim().uppercase()
            require(safeSymbol.matches(Regex("[A-Z0-9&-]+"))) {
                "Invalid stock symbol"
            }

            val connection = try {
                URL("$baseUrl/market/quote/$safeSymbol").openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw BackendApiException(0, "Unable to connect to market quote service")
            }

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                val status = try {
                    connection.responseCode
                } catch (e: IOException) {
                    throw BackendApiException(0, "Market service unreachable")
                }

                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

                if (status !in 200..299) {
                    val message = extractErrorMessage(response, status)
                    throw BackendApiException(status, message)
                }

                JSONObject(response)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun getHistory(symbol: String, range: String): JSONObject =
        withContext(Dispatchers.IO) {
            val safeSymbol = symbol.trim().uppercase()
            require(safeSymbol.matches(Regex("[A-Z0-9&-]+"))) {
                "Invalid stock symbol"
            }

            val safeRange = range.trim().uppercase()
            val connection = try {
                URL("$baseUrl/market/history/$safeSymbol?range=$safeRange").openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw BackendApiException(0, "Unable to connect to market history service")
            }

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                val status = try {
                    connection.responseCode
                } catch (e: IOException) {
                    throw BackendApiException(0, "Market service unreachable")
                }

                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

                if (status !in 200..299) {
                    val message = extractErrorMessage(response, status)
                    throw BackendApiException(status, message)
                }

                JSONObject(response)
            } finally {
                connection.disconnect()
            }
        }
}
