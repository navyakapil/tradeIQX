package com.example.tradeiq_x

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BackendApi {

    private const val BASE_URL = "http://10.0.2.2:8000"

    private suspend fun post(
        endpoint: String,
        email: String,
        password: String
    ): JSONObject = withContext(Dispatchers.IO) {

        val connection = URL("$BASE_URL$endpoint")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
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

    suspend fun signup(
        email: String,
        password: String
    ): JSONObject {
        return post("/auth/signup", email, password)
    }

    suspend fun login(
        email: String,
        password: String
    ): JSONObject {
        return post("/auth/login", email, password)
    }
}