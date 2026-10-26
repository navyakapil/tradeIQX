# TradeIQX

TradeIQX is an Android paper trading and market learning platform featuring real-time market data, interactive charts, and AI-powered intelligence.

## Market Data Setup

### 1. Environment Configuration
Create a `.env` file in the `backend/` directory based on `backend/.env.example`:

```bash
# backend/.env
GROWW_API_KEY=your_groww_api_key
GROWW_API_SECRET=your_groww_api_secret
DATABASE_URL=sqlite:///./tradeiqx.db
PORT=8002
```

### 2. Running the Market Backend
From the repository root, activate the virtual environment and start the FastAPI server:

```bash
cd backend
# Windows:
.\.venv\Scripts\activate
# macOS/Linux:
source .venv/bin/activate

uvicorn main:app --reload --host 0.0.0.0 --port 8002
```

Verify backend health at:
- `http://localhost:8002/health`
- `http://localhost:8002/docs` (Swagger UI)

### 3. Android Emulator Connection
The Android app communicates with the local FastAPI backend using the standard Android emulator loopback alias:

- **Emulator Base URL**: `http://10.0.2.2:8002`
- **Physical Device**: Replace `10.0.2.2` with your development workstation's LAN IP address (e.g. `http://192.168.1.X:8002`).

### 4. Market Data API Endpoints

| Endpoint | Method | Params | Description |
|---|---|---|---|
| `/market/quote/{symbol}` | GET | `symbol` (e.g. `RELIANCE`, `TCS`) | Returns latest traded price (LTP), currency, and fetch timestamp |
| `/market/history/{symbol}` | GET | `range` (`1D`, `1W`, `1M`, `1Y`) | Returns OHLCV candle sequence for chart visualization |

Symbol normalization automatically handles uppercase, whitespace, and optional `.NS` or `NSE_` prefixes (e.g., `reliance.ns` normalizes to `RELIANCE`).

### 5. Known Limitations
- **Trading Hours**: NSE market data is actively updated during market hours (09:15 to 15:30 IST on trading days). Outside trading hours, last closing prices are returned.
- **Rate Limits**: Upstream provider enforces API call limits; the Android repository uses a bounded exponential backoff retry mechanism (max 3 attempts).
- **Simulated Fallback**: In offline development or when market credentials are unset, deterministic simulated quotes and chart candles allow full UI/UX functionality without errors.
