package com.example.tradeiq_x

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val statusCode: Int? = null) : ApiResult<Nothing>()
}

object ApiClient {
    var BASE_URL = "http://10.0.2.2:8000"
    var DEFAULT_USER_ID = "1"
    private const val TIMEOUT_MS = 30_000

    suspend fun get(path: String, userId: String = DEFAULT_USER_ID): ApiResult<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val fullUrl = if (path.startsWith("http")) path else "$BASE_URL$path"
            val url = URL(fullUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-User-Id", userId)
            }

            val statusCode = connection.responseCode
            val isSuccess = statusCode in 200..299
            val stream = if (isSuccess) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (isSuccess) {
                ApiResult.Success(responseText)
            } else {
                val errorDetail = parseErrorMessage(responseText)
                ApiResult.Error(errorDetail, statusCode)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network request failed")
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun post(path: String, jsonBody: String, userId: String = DEFAULT_USER_ID): ApiResult<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val fullUrl = if (path.startsWith("http")) path else "$BASE_URL$path"
            val url = URL(fullUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-User-Id", userId)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val statusCode = connection.responseCode
            val isSuccess = statusCode in 200..299
            val stream = if (isSuccess) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (isSuccess) {
                ApiResult.Success(responseText)
            } else {
                val errorDetail = parseErrorMessage(responseText)
                ApiResult.Error(errorDetail, statusCode)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network request failed")
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getJsonObject(path: String, userId: String = DEFAULT_USER_ID): ApiResult<JSONObject> {
        return when (val res = get(path, userId)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(JSONObject(res.data))
                } catch (e: Exception) {
                    ApiResult.Error("Failed to parse JSON object: ${e.message}")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun postJsonObject(path: String, json: JSONObject, userId: String = DEFAULT_USER_ID): ApiResult<JSONObject> {
        return when (val res = post(path, json.toString(), userId)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(JSONObject(res.data))
                } catch (e: Exception) {
                    ApiResult.Error("Failed to parse JSON object: ${e.message}")
                }
            }
            is ApiResult.Error -> res
        }
    }

    private fun parseErrorMessage(raw: String): String {
        return try {
            val obj = JSONObject(raw)
            when {
                obj.has("detail") -> obj.getString("detail")
                obj.has("message") -> obj.getString("message")
                else -> raw
            }
        } catch (_: Exception) {
            raw.ifBlank { "Unknown server error" }
        }
    }
}
