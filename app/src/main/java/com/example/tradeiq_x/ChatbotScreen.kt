
package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
                text = "Hi! I'm TradeIQX AI 👋\n\n" +
                        "Ask me about stocks, market orders, stop-losses, " +
                        "diversification, and risk management.",
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
        val lastIndex = if (isLoading) messages.size else messages.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ChatBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("‹ Back", color = ChatGreen, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.size(8.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(ChatGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "TradeIQX AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatText
                    )
                    Text(
                        "Your market learning assistant",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))

            // Conversation
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
                            color = if (message.isUser) ChatGreen else Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(0.86f)
                        ) {
                            Text(
                                text = message.text,
                                color = if (message.isUser) Color.White else ChatText,
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
                                "  TradeIQX AI is thinking...",
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

            // Readable error message
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFDC2626),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Input and send button
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
                            textStyle = TextStyle(
                                color = ChatText,
                                fontSize = 14.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (input.isEmpty()) {
                                        Text(
                                            "Ask about stocks...",
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
                                messages.add(ChatMessage(question, true))
                                input = ""
                                isLoading = true
                                errorMessage = null

                                scope.launch {
                                    try {
                                        // Uses AIService.kt and the port 8001 API.
                                        val answer = AIService.askMentor(question)
                                        messages.add(ChatMessage(answer, false))
                                    } catch (e: Exception) {
                                        errorMessage =
                                            "AI connection failed: " +
                                                    (e.message ?: "Please try again.")
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
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "↑",
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
