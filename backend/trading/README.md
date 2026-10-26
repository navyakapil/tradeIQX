# TradeIQX Paper Trading Engine

The TradeIQX Trading Engine is an autonomous, server-authoritative backend service providing realistic paper trading, atomic order execution, position tracking, and portfolio valuation for the TradeIQX Android application.

---

## 1. How to Run the Trading Service

### Prerequisites
- Python 3.10+
- Installed packages: `pip install -r requirements.txt`

### Starting the Server
From the project root:
```bash
cd backend/trading
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
The service will be accessible locally at `http://127.0.0.1:8000` and via Android emulator at `http://10.0.2.2:8000`.

Interactive OpenAPI documentation is available at `http://127.0.0.1:8000/docs`.

---

## 2. API Endpoints

All trading endpoints require the `X-User-Id` HTTP header to identify the trader account (defaults to `default_user` if omitted).

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Service health check returning `{"status": "healthy", "service": "trading-engine"}` |
| `GET` | `/quote/{symbol}` | Authoritative price quote for a symbol (e.g. `/quote/RELIANCE`) |
| `POST` | `/orders` | Places a market order (`BUY` or `SELL`) with server price verification and atomic debit/credit |
| `GET` | `/portfolio` | Computes user portfolio valuation, cash balance, invested capital, unrealized/realized P&L, and returns |
| `GET` | `/transactions` | Filterable and sortable execution history (`symbol`, `side`, `sort=newest|oldest|amount_high|amount_low`) |

---

## 3. How Persistence Works

- **User Scoping**: Every request resolves the account via `X-User-Id`. In the Android app, this header automatically binds to the hardware Android Device ID (`Settings.Secure.ANDROID_ID`), ensuring that holdings, cash balances, and trade records persist across logouts, app restarts, and reinstalls.
- **Relational Storage**: Backed by SQLite (`trading.db`) using SQLAlchemy 2.0 ORM.
  - `trading_accounts`: Tracks user cash balance (default starting capital: ₹10,00,000.00) and cumulative realized P&L.
  - `trading_holdings`: Tracks active share quantities and weighted-average purchase prices. Positions reduced to 0 are cleanly removed from active holdings.
  - `trading_orders`: Records all order submissions with status and idempotency keys to prevent duplicate executions from retries.
  - `trading_trades`: Immutable audit log of executed trades for transaction history.
- **Atomic Transactions**: All balance deductions, average price recalculations, and trade logs execute in an isolated database transaction; failures trigger full rollback to prevent partial states.

---

## 4. Worked Manual P&L Example (Verification Walkthrough)

To verify the mathematical accuracy of the trading engine calculations, follow this step-by-step example:

### Initial Setup
- **Initial Account Cash Balance**: ₹10,00,000.00
- **Holdings**: None (0 shares)
- **Cumulative Realized P&L**: ₹0.00

---

### Step 1: Buy 10 shares of ABC @ ₹100.00
- **Cost**: $10 \times 100 = ₹1,000.00$
- **Cash Balance**: $10,00,000.00 - 1,000.00 = \mathbf{₹9,99,000.00}$
- **Holding**: 10 shares
- **Average Buy Price**: $\mathbf{₹100.00}$

---

### Step 2: Buy 10 additional shares of ABC @ ₹120.00
- **Cost**: $10 \times 120 = ₹1,200.00$
- **Cash Balance**: $9,99,000.00 - 1,200.00 = \mathbf{₹9,97,800.00}$
- **Total Shares**: $10 + 10 = \mathbf{20\text{ shares}}$
- **Weighted Average Buy Price**:
  $$\text{New Average} = \frac{(10 \times 100) + (10 \times 120)}{20} = \frac{1,000 + 1,200}{20} = \frac{2,200}{20} = \mathbf{₹110.00}$$

---

### Step 3: Sell 5 shares of ABC @ ₹130.00
- **Sale Proceeds**: $5 \times 130 = ₹650.00$
- **Cash Balance**: $9,97,800.00 + 650.00 = \mathbf{₹9,98,450.00}$
- **Realized P&L on Sold Shares**:
  $$\text{Realized P&L} = (\text{Sell Price} - \text{Average Price}) \times \text{Sold Quantity} = (130 - 110) \times 5 = 20 \times 5 = \mathbf{+₹100.00}$$
- **Account Cumulative Realized P&L**: $\mathbf{+₹100.00}$
- **Remaining Shares**: $20 - 5 = \mathbf{15\text{ shares}}$
- **Remaining Average Buy Price**: Unchanged at $\mathbf{₹110.00}$

---

### Step 4: Portfolio Valuation at Market Price ₹130.00
Assuming current market quote is ₹130.00 per share:
1. **Total Invested Capital**:
   $$\text{Invested} = 15 \times 110.00 = \mathbf{₹1,650.00}$$
2. **Current Holdings Value**:
   $$\text{Current Value} = 15 \times 130.00 = \mathbf{₹1,950.00}$$
3. **Unrealized P&L**:
   $$\text{Unrealized P&L} = \text{Current Value} - \text{Invested} = 1,950.00 - 1,650.00 = \mathbf{+₹300.00}$$
4. **Total P&L**:
   $$\text{Total P&L} = \text{Realized P&L} + \text{Unrealized P&L} = +100.00 + 300.00 = \mathbf{+₹400.00}$$
5. **Total Portfolio Value**:
   $$\text{Portfolio Value} = \text{Cash} + \text{Current Value} = 9,98,450.00 + 1,950.00 = \mathbf{₹10,00,400.00}$$
6. **Return Percentage**:
   $$\text{Return \%} = \frac{\text{Total P&L}}{\text{Invested}} \times 100 = \frac{+400.00}{1,650.00} \times 100 = \mathbf{+24.24\%}$$
