package com.example.tradeiq_x
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
private val Green = Color(0xFF16A34A)
private val DarkGreen = Color(0xFF15803D)
private val Dark = Color(0xFF0F172A)
private val Background = Color(0xFFF5F7FB)
private val Grey = Color(0xFF64748B)
private val Red = Color(0xFFDC2626)



eenum class AppScreen {
    WELCOME,
    LOGIN,
    SIGNUP,
    HOME,
    MARKET_WATCH,
    STOCK_DETAILS,
    BUY,
    SELL,
    PORTFOLIO,
    TRANSACTIONS
}




class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("TradeIQX", "MainActivity onCreate")

        setContent {
            MaterialTheme {
                TradeIQXApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("TradeIQX", "MainActivity onResume")
    }

    override fun onPause() {
        android.util.Log.d("TradeIQX", "MainActivity onPause")
        super.onPause()
    }
}


@Composable
fun TradeIQXApp() {
    var screen by remember { mutableStateOf(AppScreen.WELCOME) }

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
                onTransactions = { screen = AppScreen.TRANSACTIONS }
            )
            AppScreen.MARKET_WATCH -> MarketWatchScreen(
                onStockSelected = { screen = AppScreen.STOCK_DETAILS },
                onBack = { screen = AppScreen.HOME }
            )

            AppScreen.STOCK_DETAILS -> StockDetailsScreen(
                onBuy = { screen = AppScreen.BUY },
                onSell = { screen = AppScreen.SELL },
                onBack = { screen = AppScreen.HOME }
            )

            AppScreen.BUY -> BuyScreen(
                onBack = { screen = AppScreen.STOCK_DETAILS },
                onSell = { screen = AppScreen.SELL }
            )

            AppScreen.SELL -> SellScreen(
                onBack = { screen = AppScreen.STOCK_DETAILS }
            )

            AppScreen.PORTFOLIO -> PortfolioScreen(
                onBackClick = { screen = AppScreen.HOME }
            )

            AppScreen.TRANSACTIONS -> TransactionsScreen(
                onBackClick = { screen = AppScreen.HOME }
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

        Text(
            "Log in to continue paper trading",
            color = Grey
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green
            )
        ) {
            Text("Login")
        }

        TextButton(onClick = onSignUp) {
            Text("New here? Create an account", color = DarkGreen)
        }

        Text(
            "Demo only: login currently opens the dashboard.",
            color = Grey,
            fontSize = 12.sp
        )
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
    var error by remember { mutableStateOf("") }

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

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Red, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                error = when {
                    name.isBlank() || email.isBlank() ||
                            password.isBlank() || confirmPassword.isBlank() ->
                        "Please fill in all fields."

                    !email.contains("@") ->
                        "Enter a valid email address."

                    password.length < 6 ->
                        "Password must be at least 6 characters."

                    password != confirmPassword ->
                        "Passwords do not match."

                    else -> ""
                }

                if (error.isEmpty()) onCreateAccount()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green
            )
        ) {
            Text("Create Account")
        }

        TextButton(onClick = onLogin) {
            Text("Already have an account? Login", color = DarkGreen)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onMarketWatch: () -> Unit,
    onPortfolio: () -> Unit,
    onTransactions: () -> Unit
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
                Text("TradeIQX", color = Green, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
                Text("Welcome to your dashboard", color = Grey,
                    fontSize = 13.sp)
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
            colors = CardDefaults.cardColors(
                containerColor = Dark
            )
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Available Virtual Balance", color = Color.LightGray)
                Spacer(Modifier.height(8.dp))
                Text(
                    "₹10,00,000",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
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
        QuickAccessCard("💼", "My Portfolio", "View your holdings",onClick = onPortfolio)
        QuickAccessCard("↔", "Transactions", "Review your paper trades",onClick = onTransactions)

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
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
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
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
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
