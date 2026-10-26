
package com.example.tradeiq_x

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BackendApi {
    private const val BASE_URL = "http://10.0.2.2:8001"

    private suspend fun post(
        endpoint: String,
        data: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL("$BASE_URL$endpoint")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            connection.outputStream.use {
                it.write(data.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use {
                it.readText()
            } ?: "{}"

            if (status !in 200..299) {
                val message = try {
                    JSONObject(response).optString(
                        "detail",
                        "Request failed: $status"
                    )
                } catch (_: Exception) {
                    "Request failed: $status"
                }
                throw Exception(message)
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
        put("email", email)
        put("password", password)
    }


    suspend fun signup(
        username: String,
        email: String,
        password: String
    ): JSONObject {
        val data = JSONObject().apply {
            put("username", username.trim())
            put("email", email.trim())
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

    suspend fun getLiveQuote(symbol: String): JSONObject =
        withContext(Dispatchers.IO) {
            val safeSymbol = symbol.trim().uppercase()
            require(safeSymbol.matches(Regex("[A-Z0-9]+"))) {
                "Invalid stock symbol"
            }

            val connection = URL(
                "$BASE_URL/market/quote/$safeSymbol"
            ).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use {
                    it.readText()
                } ?: "{}"

                if (status !in 200..299) {
                    val message = try {
                        JSONObject(response).optString(
                            "detail",
                            "Quote request failed: $status"
                        )
                    } catch (_: Exception) {
                        "Quote request failed: $status"
                    }
                    throw Exception(message)
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
            val connection = URL(
                "$BASE_URL/market/history/$safeSymbol?range=$safeRange"
            ).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use {
                    it.readText()
                } ?: "{}"

                if (status !in 200..299) {
                    val message = try {
                        JSONObject(response).optString(
                            "detail",
                            "History request failed: $status"
                        )
                    } catch (_: Exception) {
                        "History request failed: $status"
                    }
                    throw Exception(message)
                }

                JSONObject(response)
            } finally {
                connection.disconnect()
            }
        }
}
