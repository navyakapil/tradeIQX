package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val ChatGreen = Color(0xFF16A34A)
private val ChatBackground = Color(0xFFF6F8FA)
private val ChatText = Color(0xFF172033)

private data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatbotScreen(
    onBack: () -> Unit = {}
) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Hi! I'm TradeIQX AI 👋\n\nAsk me about stock market concepts, investing basics, or financial terminology. How can I help you today?",
                isUser = false
            )
        )
    }

    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                if (isLoading) messages.size else messages.lastIndex
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ChatBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        text = "‹ Back",
                        color = ChatGreen,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(ChatGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.size(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TradeIQX AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatText
                    )

                    Text(
                        text = "Your market learning assistant",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))

            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isUser) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (message.isUser) 16.dp else 4.dp,
                                bottomEnd = if (message.isUser) 4.dp else 16.dp
                            ),
                            color = if (message.isUser) {
                                ChatGreen
                            } else {
                                Color.White
                            },
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(0.86f)
                        ) {
                            Text(
                                text = message.text,
                                color = if (message.isUser) {
                                    Color.White
                                } else {
                                    ChatText
                                },
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ChatGreen,
                                strokeWidth = 2.dp
                            )

                            Text(
                                text = "  TradeIQX AI is thinking...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Error message
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFDC2626),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Message input
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = ChatBackground
                    ) {
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            enabled = !isLoading,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = ChatText,
                                fontSize = 14.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (input.isEmpty()) {
                                        Text(
                                            text = "Ask anything about stocks...",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Button(
                        onClick = {
                            val question = input.trim()

                            if (question.isNotEmpty() && !isLoading) {
                                messages.add(
                                    ChatMessage(question, true)
                                )

                                input = ""
                                isLoading = true
                                errorMessage = null

                                scope.launch {
                                    try {
                                        val answer = withContext(Dispatchers.IO) {
                                            askTradeIQXAI(question)
                                        }

                                        messages.add(
                                            ChatMessage(answer, false)
                                        )
                                    } catch (e: Exception) {
                                        errorMessage =
                                            "Couldn't connect to the AI. Please try again."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = input.isNotBlank() && !isLoading,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChatGreen
                        ),
                        modifier = Modifier.size(48.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "↑",
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/*
 * Sends the user's question to your backend.
 *
 * Expected request:
 * {"message": "What is a stock?"}
 *
 * Expected response:
 * {"reply": "A stock represents ownership in a company."}
 */
private fun askTradeIQXAI(question: String): String {
    // Android emulator accesses your computer's localhost using 10.0.2.2.
    // Change the port/path to match your actual backend.
    val url = URL("http://10.0.2.2:5000/chat")

    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.doOutput = true

        connection.setRequestProperty(
            "Content-Type",
            "application/json; charset=UTF-8"
        )
        connection.setRequestProperty("Accept", "application/json")

        val requestBody = JSONObject()
            .put("message", question)
            .toString()

        connection.outputStream.use { output ->
            output.write(requestBody.toByteArray(Charsets.UTF_8))
        }

        val statusCode = connection.responseCode

        val responseStream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseText = responseStream?.bufferedReader()?.use {
            it.readText()
        }.orEmpty()

        if (statusCode !in 200..299) {
            throw Exception("AI server returned HTTP $statusCode")
        }

        val json = JSONObject(responseText)

        return json.optString(
            "reply",
            "Sorry, I couldn't generate a response."
        )
    } finally {
        connection.disconnect()
    }
}