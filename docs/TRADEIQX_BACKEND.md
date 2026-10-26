# TradeIQX Backend & Android Integration Guide

This document covers backend setup, endpoints, architectural design, database schemas, financial P&L calculation formulas, persistence guarantees, and in-app verification steps.

---

## 1. Setup & Running the Backend

### Prerequisites
- Python 3.10+
- SQLite3

### Environment Configuration (.env)
Copy `backend/.env.example` to `backend/.env` and configure credentials:
```bash
cd backend
cp .env.example .env
```
Key configuration variables in `backend/.env`:
```env
LLM_API_KEY=your_gemini_or_openai_api_key_here
LLM_MODEL=gemini-2.5-flash
DATABASE_URL=sqlite:///./tradeiqx.db
RAG_TOP_K=3
RAG_THRESHOLD=0.30
```
> **Security Notice**: Never commit `backend/.env` or any real API keys. Keys reside exclusively in `backend/.env` on the backend server and are never delivered to Android or GitHub.

### Start Backend Server
```bash
cd backend
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
The server will start on `http://localhost:8000` (or `http://0.0.0.0:8000`).

---

## 2. Android Emulator Networking
- In the Android Emulator, `10.0.2.2` maps to `127.0.0.1` on the development host.
- Base URL configured in `ApiClient.kt`:
  ```
  http://10.0.2.2:8000
  ```
- Cleartext HTTP for `10.0.2.2` and `localhost` is explicitly permitted in `network_security_config.xml`.

---

## 3. API Endpoints Reference

### AI Trading Mentor (RAG)
- **`POST /api/chat`**
  - **Request**: `{"question": "What is a stop-loss?"}` (or `{"message": "..."}`)
  - **Validation**: Non-empty, max 500 characters.
  - **Response**: `{"answer": "...", "sources": ["Stop Loss", "Trailing Stop-Loss and Placement"], "reply": "..."}`
  - Out-of-scope queries (unrelated to trading) are short-circuited before calling the LLM.

### Health Check & System Status
- **`GET /api/health`** (and `GET /health`)
  - **Response**: `{"status": "healthy", "database": "healthy"}`

### Trading Engine & Orders
- **`POST /api/trade/buy`** (and `POST /buy`)
  - **Header**: `X-User-Id: <user_id>` (defaults to demo user `1`)
  - **Request**: `{"symbol": "RELIANCE", "quantity": 10, "price": 1450.25, "client_order_id": "optional_id"}`
  - **Validation**: `quantity > 0`, `price > 0`, `symbol` in known traded universe, total cost <= available cash balance.
  - **Response**: `TradeOrderResponse` with updated holding and remaining virtual cash.
  
- **`POST /api/trade/sell`** (and `POST /sell`)
  - **Header**: `X-User-Id: <user_id>`
  - **Request**: `{"symbol": "RELIANCE", "quantity": 5, "price": 1500.00, "client_order_id": "optional_id"}`
  - **Validation**: `quantity > 0`, `quantity <= owned_shares`.
  - **Response**: `TradeOrderResponse` with realized P&L and remaining quantity.

### Portfolio & Transaction Analytics
- **`GET /api/portfolio`** (and `GET /portfolio`)
  - **Header**: `X-User-Id: <user_id>`
  - **Response**:
    - `cash`: Available virtual funds.
    - `total_invested`: Sum of `quantity * avg_price` for all open holdings.
    - `current_value`: Sum of `quantity * current_market_price`.
    - `portfolio_value`: `cash + current_value`.
    - `unrealized_pnl`: `current_value - total_invested`.
    - `realized_pnl`: Sum of realized profits/losses from all executed sells.
    - `total_pnl`: `realized_pnl + unrealized_pnl`.
    - `return_pct`: Zero-safe percentage return on invested capital.
    - `holdings_detail`: Per-holding breakdown with individual P&L and return %.

- **`GET /api/transactions`** (and `GET /transactions`)
  - **Query Params**:
    - `side`: `ALL` (default), `BUY`, `SELL`
    - `symbol`: Substring filter (e.g. `Reliance`)
    - `sort`: `newest` (default), `oldest`, `amount_desc`, `amount_asc`
  - **Response**: List of executed order transactions with readable timestamps and realized P&L on sell rows.

---

## 4. Database Schema (SQLite)

Located at `backend/tradeiqx.db`:
- **`users`**: `id`, `username`, `email`, `hashed_password`, `created_at`
- **`accounts`**: `id`, `user_id` (FK -> users.id), `cash` (default ₹10,00,000.00)
- **`portfolios`**: `id`, `user_id` (FK -> users.id), `cash_balance`
- **`holdings`**: `id`, `user_id` (FK -> users.id), `symbol`, `quantity`, `average_price` (`avg_buy_price`), unique constraint on `(user_id, symbol)`
- **`orders`**: `id`, `user_id`, `client_order_id`, `side`, `symbol`, `quantity`, `price`, `status`, `created_at`
- **`trades`**: `id`, `user_id`, `order_id` (FK -> orders.id), `symbol`, `side`, `quantity`, `price`, `total`, `realized_pnl`, `created_at`

---

## 5. Financial P&L Formulas

1. **Weighted Average Buy Price (Holding Cost Basis)**:
   $$\text{New Avg Price} = \frac{(\text{Old Qty} \times \text{Old Avg Price}) + (\text{Buy Qty} \times \text{Buy Price})}{\text{Old Qty} + \text{Buy Qty}}$$

2. **Realized Profit & Loss (Recorded on Sell)**:
   $$\text{Realized P&L} = \text{Sell Quantity} \times (\text{Sell Price} - \text{Holding Avg Buy Price})$$

3. **Unrealized Profit & Loss**:
   $$\text{Unrealized P&L} = \text{Total Market Value} - \text{Total Invested}$$

4. **Return Percentage (Zero-Safe)**:
   $$\text{Return \%} = \begin{cases} \left(\frac{\text{Unrealized P&L}}{\text{Total Invested}}\right) \times 100, & \text{if Total Invested} > 0 \\ 0.0, & \text{otherwise} \end{cases}$$

5. **Pruning Empty Holdings**:
   When a sell order reduces `holding.quantity` to `0`, the holding record is deleted from the active portfolio database table.

---

## 6. Persistence Across Restart, Logout, and Reinstall
- **Backend Single Source of Truth**: All balances, holdings, orders, and executed trades persist in `tradeiqx.db`.
- **User Identification**: The app sends a persistent demo identifier via the `X-User-Id` HTTP header. Because state is keyed to the user ID in the SQLite database, data remains intact after app restart, logout, or reinstallation.
- **Client-Side Cache**: `TradingStore.kt` automatically saves the last portfolio summary and transaction history into local `SharedPreferences`. If network access is temporarily interrupted, cached figures are shown alongside a reconnect/retry option.

---

## 7. In-App Manual Testing Checklist

### A) AI Trading Mentor
1. Open TradeIQX -> Tap **"AI Trading Mentor"** on Home screen.
2. Ask: *"What is a stop-loss?"* -> Verify AI bubble explains stop-loss with sources badge.
3. Ask: *"What is the difference between market and limit orders?"* -> Verify explanation of execution certainty vs price certainty.
4. Ask: *"How to make pancakes?"* -> Verify out-of-scope short-circuit: *"I only help with trading, investing, and financial market concepts..."*
5. Tap one of the suggested query chips -> Verify automatic dispatch and answer display.

### B) Paper Trading & Execution
1. Navigate to **Market Watch** -> Tap **RELIANCE**.
2. Tap **Buy** -> Enter quantity `10` -> Confirm buy order.
3. Verify virtual cash is deducted, holding created, and app routes to Portfolio.
4. Tap **RELIANCE** again -> Tap **Buy** -> Enter quantity `10` @ higher/lower price -> Verify weighted average price updates correctly.
5. Tap **Sell** -> Sell `5` shares -> Verify realized P&L is calculated and proceeds credited.
6. Attempt to sell more shares than owned -> Verify error message *"Insufficient shares to sell"*.
7. Attempt to buy more shares than available cash balance -> Verify error message *"Insufficient virtual cash"*.

### C) Portfolio Analytics
1. Open **My Portfolio**.
2. Verify Total Portfolio Value = `Available Cash + Total Holdings Value`.
3. Verify sign-aware green/red formatting for P&L (+ green for profit, - red for loss).
4. Verify individual holding cards show shares, avg buy price, current price, and % return.

### D) Transactions History
1. Open **Transactions** screen.
2. Toggle chips: `ALL`, `BUY`, `SELL` -> Verify list filters accordingly.
3. Test Sort chips: `Newest`, `Oldest`, `Amount ↓`, `Amount ↑`.
4. Type in Search bar (e.g. *"Reliance"*) -> Verify results filter by symbol.
5. Verify SELL rows show green/red realized P&L.

---

## 8. Shared Files Modified

The following shared project files were modified additively with backward compatibility:
1. `backend/main.py`: Added RAG chat endpoint, CORS, startup preloading, health check, duplicate order validation, static price integration, and transaction filtering.
2. `backend/models.py`: Added `Account` and `Order` models; added `avg_buy_price` synonym and `order_id` / `realized_pnl` columns.
3. `backend/schemas.py`: Added `TradeOrderResponse`, `HoldingDetail`, `PortfolioSummaryResponse`, and `client_order_id` support.
4. `backend/requirements.txt`: Added missing python dependencies (`sentence-transformers`, `scikit-learn`, `python-dotenv`, `google-genai`).
5. `.gitignore` & `backend/.gitignore`: Added rules for embedding cache files, `.env`, and database files.
6. `backend/.env.example`: Added configuration keys for `LLM_API_KEY`, `LLM_MODEL`, `RAG_TOP_K`, and `RAG_THRESHOLD`.
7. `app/src/main/AndroidManifest.xml`: Configured cleartext network security for emulator `10.0.2.2` and localhost.
8. `app/src/main/java/com/example/tradeiq_x/MainActivity.kt`: Added selected stock price plumbing to Buy/Sell navigation and updated AI Mentor Home card.
9. `app/src/main/java/com/example/tradeiq_x/ChatbotScreen.kt`: Wired messaging to `ApiClient`, added retry handler, loading state, and sources badge.
10. `app/src/main/java/com/example/tradeiq_x/TradingStore.kt`: Connected to backend trading endpoints with persistent local SharedPreferences caching.
11. `app/src/main/java/com/example/tradeiq_x/BuyScreen.kt` & `SellScreen.kt`: Wired to `TradingStore` execution with server validation feedback, duplicate order guard, and loading states.
12. `app/src/main/java/com/example/tradeiq_x/PortfolioScreen.kt` & `TransactionsScreen.kt`: Replaced sample data with real-time analytics, filtering, search, and sorting.
