
package com.example.tradeiq_x

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray

@Composable
fun QuizScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var topic by remember { mutableStateOf("Stock Market Basics") }
    var difficulty by remember { mutableStateOf("Beginner") }
    var questions by remember { mutableStateOf<JSONArray?>(null) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }

    fun startQuiz() {
        scope.launch {
            loading = true
            error = null
            questions = null
            finished = false
            score = 0
            currentIndex = 0
            selectedIndex = -1

            try {
                val result = AIService.generateQuiz(topic, difficulty)
                questions = result.getJSONArray("questions")
            } catch (e: Exception) {
                error = e.message ?: "Could not generate quiz."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to AI Mentor")
        }

        Text(
            text = "TradeIQX AI Quiz",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("AI is generating your questions...")
        } else if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = { startQuiz() }) {
                Text("Try Again")
            }
        } else if (finished) {
            val total = questions?.length() ?: 0

            Text(
                "Quiz Complete!",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(12.dp))
            Text("Your score: $score / $total")

            Spacer(Modifier.height(20.dp))
            Button(onClick = { startQuiz() }) {
                Text("Try Another Quiz")
            }
        } else if (questions == null) {

            Text("Topic")
            Spacer(Modifier.height(8.dp))

            listOf(
                "Stock Market Basics",
                "Technical Analysis",
                "Risk Management"
            ).forEach { choice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = topic == choice,
                            onClick = { topic = choice }
                        )
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = topic == choice,
                        onClick = { topic = choice }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(choice)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Difficulty")

            listOf(
                "Beginner",
                "Intermediate",
                "Advanced"
            ).forEach { choice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = difficulty == choice,
                            onClick = { difficulty = choice }
                        )
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = difficulty == choice,
                        onClick = { difficulty = choice }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(choice)
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { startQuiz() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate AI Quiz")
            }

        } else {
            val quizQuestions = questions!!

            if (currentIndex < quizQuestions.length()) {
                val question = quizQuestions.getJSONObject(currentIndex)
                val options = question.getJSONArray("options")
                val correctIndex = question.getInt("correct_index")
                val explanation = question.getString("explanation")

                Text(
                    "Question ${currentIndex + 1} of ${quizQuestions.length()}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    question.getString("question"),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(
                        (0 until options.length()).toList()
                    ) { _, index ->
                        val option = options.getString(index)

                        OutlinedButton(
                            onClick = {
                                if (selectedIndex == -1) {
                                    selectedIndex = index
                                    if (index == correctIndex) {
                                        score++
                                    }
                                }
                            },
                            enabled = selectedIndex == -1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(option)
                        }
                    }
                }

                if (selectedIndex != -1) {
                    Text(
                        if (selectedIndex == correctIndex)
                            "Correct!"
                        else
                            "Incorrect. Correct answer: ${options.getString(correctIndex)}"
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Explanation: $explanation")

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (currentIndex + 1 >= quizQuestions.length()) {
                                finished = true
                            } else {
                                currentIndex++
                                selectedIndex = -1
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (currentIndex + 1 == quizQuestions.length())
                                "See Final Score"
                            else
                                "Next Question"
                        )
                    }
                }
            }
        }
    }
}


