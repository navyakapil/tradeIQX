
package com.example.tradeiq_x

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Green = Color(0xFF16A34A)
private val DarkGreen = Color(0xFF15803D)
private val Dark = Color(0xFF0F172A)
private val Background = Color(0xFFF5F7FB)
private val Grey = Color(0xFF64748B)
private val ErrorRed = Color(0xFFDC2626)

enum class AppScreen {
    WELCOME, LOGIN, SIGNUP, HOME, MARKET_WATCH,
    STOCK_DETAILS, BUY, SELL, PORTFOLIO, TRANSACTIONS, CHATBOT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TradeIQXApp()
            }
        }
    }
}

@Composable
fun TradeIQXApp() {
    var screen by remember { mutableStateOf(AppScreen.WELCOME) }
    var selectedStock by remember { mutableStateOf("RELIANCE") }
    var selectedStockPrice by remember { mutableStateOf<Double?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        when (screen) {
            AppScreen.WELCOME -> WelcomeScreen {
                screen = AppScreen.LOGIN
            }

            AppScreen.LOGIN -> LoginScreen(
                onLogin = { screen = AppScreen.HOME },
                onSignUp = { screen = AppScreen.SIGNUP }
            )

            AppScreen.SIGNUP -> SignUpScreen(
                onCreateAccount = { screen = AppScreen.HOME },
                onLogin = { screen = AppScreen.LOGIN }
            )

            AppScreen.HOME -> HomeScreen(
                onLogout = { screen = AppScreen.WELCOME },
                onMarketWatch = { screen = AppScreen.MARKET_WATCH },
                onPortfolio = { screen = AppScreen.PORTFOLIO },
                onTransactions = { screen = AppScreen.TRANSACTIONS },
                onChatbot = { screen = AppScreen.CHATBOT }
            )

            AppScreen.MARKET_WATCH -> MarketWatchScreen(
                onStockSelected = { symbol ->
                    selectedStock = symbol
                    screen = AppScreen.STOCK_DETAILS
                },
                onBack = { screen = AppScreen.HOME }
            )

            AppScreen.STOCK_DETAILS -> StockDetailsScreen(
                symbol = selectedStock,
                onBuy = {
                    selectedStockPrice = TradingStore.prices[selectedStock.uppercase()]
                    screen = AppScreen.BUY
                },
                onSell = {
                    selectedStockPrice = TradingStore.prices[selectedStock.uppercase()]
                    screen = AppScreen.SELL
                },
                onBack = { screen = AppScreen.MARKET_WATCH }
            )

            AppScreen.BUY -> BuyScreen(
                symbol = selectedStock,
                stockPrice = selectedStockPrice,
                onBack = { screen = AppScreen.STOCK_DETAILS },
                onSell = { screen = AppScreen.SELL },
                onOrderSuccess = { screen = AppScreen.PORTFOLIO }
            )

            AppScreen.SELL -> SellScreen(
                symbol = selectedStock,
                stockPrice = selectedStockPrice,
                onBack = { screen = AppScreen.STOCK_DETAILS },
                onOrderSuccess = { screen = AppScreen.PORTFOLIO }
            )

            AppScreen.PORTFOLIO -> PortfolioScreen(
                onBackClick = { screen = AppScreen.HOME },
                onTransactionsClick = {
                    screen = AppScreen.TRANSACTIONS
                }
            )

            AppScreen.TRANSACTIONS -> TransactionsScreen(
                onBackClick = { screen = AppScreen.PORTFOLIO }
            )

            AppScreen.CHATBOT -> ChatbotScreen(
                onBack = { screen = AppScreen.HOME }
            )
        }
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "↗",
            color = Color(0xFF22C55E),
            fontSize = 76.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "TradeIQX",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Learn. Practice. Trade Smarter.",
            color = Color(0xFFCBD5E1),
            fontSize = 15.sp
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Your virtual trading companion",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green
            )
        ) {
            Text("Get Started", fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun validateForm(): String? {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) return "Please enter your email address."
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return "Please enter a valid email address."
        }
        if (password.isEmpty()) return "Please enter your password."
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandHeader()
        Spacer(Modifier.height(32.dp))

        Text(
            "Welcome back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Dark
        )

        Text("Log in to continue paper trading", color = Grey)
        Spacer(Modifier.height(28.dp))

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEE2E2)
                )
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (errorMessage != null) errorMessage = null
            },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (errorMessage != null) errorMessage = null
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isLoading) return@Button
                val validationError = validateForm()
                if (validationError != null) {
                    errorMessage = validationError
                    return@Button
                }
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        BackendApi.login(email.trim(), password)
                        isLoading = false
                        onLogin()
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.message ?: "Authentication failed. Please verify credentials."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onSignUp,
            enabled = !isLoading
        ) {
            Text(
                "New here? Create an account",
                color = DarkGreen
            )
        }
    }
}

@Composable
fun SignUpScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun validateSignUpForm(): String? {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isEmpty()) return "Please enter your username."
        if (trimmedName.length < 3) return "Username must be at least 3 characters."
        if (trimmedName.length > 50) return "Username cannot exceed 50 characters."
        if (!trimmedName.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return "Username can only contain letters, numbers, underscores, and hyphens."
        }

        if (trimmedEmail.isEmpty()) return "Please enter your email address."
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return "Please enter a valid email address."
        }

        if (password.length < 8) return "Password must be at least 8 characters."
        if (password.length > 128) return "Password cannot exceed 128 characters."
        if (!password.any { it.isDigit() }) return "Password must contain at least one number."
        if (!password.any { it.isLetter() }) return "Password must contain at least one letter."

        if (password != confirmPassword) return "Passwords do not match."

        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandHeader()
        Spacer(Modifier.height(24.dp))

        Text(
            "Create account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Dark
        )

        Text("Start your paper-trading journey", color = Grey)
        Spacer(Modifier.height(24.dp))

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEE2E2)
                )
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (errorMessage != null) errorMessage = null
            },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (errorMessage != null) errorMessage = null
            },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (errorMessage != null) errorMessage = null
            },
            label = { Text("Password (min 8 chars, 1 letter, 1 number)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                if (errorMessage != null) errorMessage = null
            },
            label = { Text("Confirm password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (isLoading) return@Button
                val validationError = validateSignUpForm()
                if (validationError != null) {
                    errorMessage = validationError
                    return@Button
                }
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        BackendApi.signup(name.trim(), email.trim(), password)
                        isLoading = false
                        onCreateAccount()
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.message ?: "Registration failed. Please check your details."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Account")
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onLogin,
            enabled = !isLoading
        ) {
            Text(
                "Already have an account? Login",
                color = DarkGreen
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onMarketWatch: () -> Unit,
    onPortfolio: () -> Unit,
    onTransactions: () -> Unit,
    onChatbot: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "TradeIQX",
                    color = Green,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Welcome to your dashboard",
                    color = Grey,
                    fontSize = 13.sp
                )
            }

            TextButton(onClick = onLogout) {
                Text("Logout", color = DarkGreen)
            }
        }

        Spacer(Modifier.height(26.dp))

        Text(
            "Hello, Trader 👋",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Dark
        )

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Dark)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Available Virtual Balance", color = Color.LightGray)
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "₹" + java.text.NumberFormat
                        .getNumberInstance(java.util.Locale("en", "IN"))
                        .format(TradingStore.balance.value),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Practice with virtual money",
                    color = Color(0xFF86EFAC)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Portfolio Value",
                value = "₹10,00,000",
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "Today's P&L",
                value = "₹0.00",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(26.dp))

        Text(
            "Quick Access",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Dark
        )

        Spacer(Modifier.height(12.dp))

        QuickAccessCard(
            "📈",
            "Market Watch",
            "Explore available shares",
            onClick = onMarketWatch
        )

        QuickAccessCard(
            "💼",
            "My Portfolio",
            "View your holdings",
            onClick = onPortfolio
        )

        QuickAccessCard(
            "↔",
            "Transactions",
            "Review your paper trades",
            onClick = onTransactions
        )

        QuickAccessCard(
            "🤖",
            "AI Trading Mentor",
            "Ask questions about stocks and investing",
            onClick = onChatbot
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Market prices and trading features will be connected in a later stage.",
            color = Grey,
            fontSize = 12.sp
        )
    }
}

@Composable
fun BrandHeader() {
    Text(
        "↗",
        color = Green,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        "TradeIQX",
        color = Dark,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        "Learn. Practice. Trade Smarter.",
        color = Grey,
        fontSize = 13.sp
    )
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Grey, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))

            Text(
                value,
                color = Dark,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun QuickAccessCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 25.sp)
            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = Dark
                )
                Text(subtitle, fontSize = 12.sp, color = Grey)
            }
        }
    }
}
