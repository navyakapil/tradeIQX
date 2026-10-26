package com.example.tradeiq_x

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
private val ChatDark = Color(0xFF1E293B)
private val ChatMuted = Color(0xFF64748B)
private val ChatBorder = Color(0xFFE2E8F0)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val sources: List<String> = emptyList(),
    val isError: Boolean = false,
    val failedPrompt: String? = null
)

@Composable
fun ChatbotScreen(
    onBack: () -> Unit = {}
) {
    var showQuiz by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Hello! I am your TradeIQX AI Trading Mentor 👋\n\n" +
                        "Ask me anything about stock market basics, market & limit orders, " +
                        "stop-losses, diversification, or risk management.",
                isUser = false
            )
        )
    }

    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var lastQuestion by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sendQuestion: (String) -> Unit = { rawQuestion ->
        val question = rawQuestion.trim()
        if (question.isNotEmpty() && !isLoading) {
            messages.add(ChatMessage(text = question, isUser = true))
            input = ""
            isLoading = true
            lastQuestion = question

            scope.launch {
                try {
                    val reqJson = org.json.JSONObject().apply {
                        put("question", question)
                        put("message", question)
                    }
                    when (val result = ApiClient.postJsonObject("/api/chat", reqJson)) {
                        is ApiResult.Success -> {
                            val obj = result.data
                            val answer = if (obj.has("answer")) obj.getString("answer") else obj.optString("reply", "No response received.")
                            val sourcesList = mutableListOf<String>()
                            val sourcesArr = obj.optJSONArray("sources")
                            if (sourcesArr != null) {
                                for (i in 0 until sourcesArr.length()) {
                                    sourcesList.add(sourcesArr.getString(i))
                                }
                            }
                            messages.add(ChatMessage(text = answer, isUser = false, sources = sourcesList))
                        }
                        is ApiResult.Error -> {
                            messages.add(
                                ChatMessage(
                                    text = "TradeIQX AI Mentor: ${result.message}",
                                    isUser = false,
                                    isError = true,
                                    failedPrompt = question
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    messages.add(
                        ChatMessage(
                            text = "Could not connect to AI Mentor: ${e.message ?: "Connection error"}",
                            isUser = false,
                            isError = true,
                            failedPrompt = question
                        )
                    )
                } finally {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            val targetIndex = if (isLoading) messages.size else messages.size - 1
            listState.animateScrollToItem(maxOf(0, targetIndex))
        }
    }

    if (showQuiz) {
        QuizScreen(onBack = { showQuiz = false })
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ChatBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TOP APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("‹ Back", color = ChatGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.size(6.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ChatGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("IQ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.size(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Trading Mentor",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatDark
                    )
                    Text(
                        "RAG Powered • TradeIQX Knowledge",
                        fontSize = 11.sp,
                        color = ChatMuted
                    )
                }

                Button(
                    onClick = { showQuiz = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ChatGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("AI Quiz", color = Color.White, fontSize = 11.sp)
                }
            }

            HorizontalDivider(color = ChatBorder)

            // MESSAGE LIST
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                items(messages, key = { it.id }) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (message.isUser) 16.dp else 4.dp,
                                bottomEnd = if (message.isUser) 4.dp else 16.dp
                            ),
                            color = when {
                                message.isUser -> ChatGreen
                                message.isError -> Color(0xFFFEF2F2)
                                else -> Color.White
                            },
                            border = if (!message.isUser && !message.isError) {
                                androidx.compose.foundation.BorderStroke(1.dp, ChatBorder)
                            } else null,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = message.text,
                                    color = when {
                                        message.isUser -> Color.White
                                        message.isError -> Color(0xFFDC2626)
                                        else -> ChatDark
                                    },
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )

                                if (message.sources.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        message.sources.forEach { src ->
                                            Surface(
                                                color = Color(0xFFDCFCE7),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = src,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (message.isError && message.failedPrompt != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { sendQuestion(message.failedPrompt) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("↻ Retry", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ChatGreen,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Thinking… consulting TradeIQX knowledge base",
                                color = ChatMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(6.dp)) }
            }

            // SUGGESTIONS CHIPS
            if (messages.size == 1 && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Try asking:",
                        color = ChatMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val suggestions = listOf(
                        "What is the difference between market and limit orders?",
                        "What is a stop-loss and how does it protect me?",
                        "Why should I diversify my portfolio?",
                        "How much should I risk per trade?"
                    )
                    suggestions.forEach { suggestion ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sendQuestion(suggestion) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChatBorder)
                        ) {
                            Text(
                                text = "💡 $suggestion",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = ChatDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // INPUT ROW
            Surface(
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(22.dp),
                        color = ChatBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChatBorder)
                    ) {
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            enabled = !isLoading,
                            singleLine = true,
                            textStyle = TextStyle(color = ChatDark, fontSize = 14.sp),
                            decorationBox = { inner ->
                                Box {
                                    if (input.isEmpty()) {
                                        Text("Ask a trading or investing question...", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Button(
                        onClick = { sendQuestion(input) },
                        enabled = input.isNotBlank() && !isLoading,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChatGreen,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.size(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
