# TradeIQX Cloud Run & Firebase Deployment Findings

**Inspection Date**: 2026-10-27  
**Repository**: TradeIQX (Android Kotlin/Jetpack Compose + FastAPI Python Backend)  
**Target Environment**: Google Cloud Run + Firebase Hosting  

---

## 1. Backend Entry Point & Route Contract

### Entry Point
- **File**: `backend/main.py`
- **Application Instance**: `app = FastAPI(title="TradeIQX API")`

### Registered Route List
Snapshot saved to [`docs/api_routes_before.txt`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/docs/api_routes_before.txt):

| HTTP Method | Route Path | Handler / Purpose |
|---|---|---|
| `GET` | `/` | Root status message |
| `GET` | `/health` | Application health check (`{"status": "ok"}`) |
| `GET` | `/auth/me` | Fetch authenticated user profile & balance |
| `POST` | `/auth/signup` | Register new user account |
| `POST` | `/auth/login` | Authenticate user & verify password hash |
| `GET` | `/market/quote/{symbol}` | Live stock quote (LTP, change, volume, freshness) |
| `GET` | `/market/history/{symbol}` | OHLC historical candle sequence (`1D`, `1W`, `1M`, `1Y`) |
| `POST` | `/buy` | Execute buy order & update user cash balance / holdings |
| `POST` | `/sell` | Execute sell order & update user cash balance / holdings |
| `GET` | `/portfolio` | Retrieve user portfolio summary & active holdings |
| `POST` | `/analyze` | AI stock signal & indicator technical explanation |
| `POST` | `/api/chat` | AI Mentor RAG conversational trading assistant |
| `POST` | `/api/quiz/generate` | AI-powered financial quiz generator |
| `GET,HEAD` | `/docs` | OpenAPI / Swagger interactive documentation UI |
| `GET,HEAD` | `/redoc` | ReDoc alternative documentation UI |
| `GET,HEAD` | `/openapi.json` | OpenAPI schema JSON specification |
| `GET,HEAD` | `/docs/oauth2-redirect` | OAuth2 redirect handler for Swagger |

### Routes Called by the Android App
Inspection of `app/src/main/java/com/example/tradeiq_x/`:
1. `POST /auth/signup` &mdash; Called by [`BackendApi.signup`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/BackendApi.kt#L172)
2. `POST /auth/login` &mdash; Called by [`BackendApi.login`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/BackendApi.kt#L179)
3. `GET /auth/me` &mdash; Called by [`BackendApi.getProfile`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/BackendApi.kt#L183)
4. `GET /market/quote/{symbol}` &mdash; Called by [`BackendApi.getLiveQuote`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/BackendApi.kt#L187)
5. `GET /market/history/{symbol}?range={range}` &mdash; Called by [`BackendApi.getHistory`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/BackendApi.kt#L230)
6. `POST /analyze` &mdash; Called by [`AIService.getStockExplanation`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/AIService.kt#L88)
7. `POST /api/chat` &mdash; Called by [`AIService.askMentor`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/AIService.kt#L110)
8. `POST /api/quiz/generate` &mdash; Called by [`AIService.generateQuiz`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/java/com/example/tradeiq_x/AIService.kt#L152)

> **Conclusion**: Every single route called by Android exists in `backend/main.py`. No external backend service is required.

---

## 2. Environment Variables Used

Names only (no values or secrets):
- `GROWW_API_KEY`: Used in `backend/market_data.py` for Groww SDK market feed authentication.
- `GROWW_API_SECRET`: Used in `backend/market_data.py` for Groww SDK API secret.
- `DATABASE_URL`: Used in `backend/database.py` for SQLAlchemy database connection string (defaults to local SQLite).
- `PORT`: Used by Uvicorn container execution (Cloud Run injects `$PORT`, defaulting to `8080` in cloud containers).

---

## 3. Laptop-Only Dependencies & Cloud Constraints

### a. Ollama AI Hardcoded Localhost Dependency
- **Location**: `backend/rag_service.py` lines 21–25:
  - `OLLAMA_URL = "http://127.0.0.1:11434"`
  - `CHAT_MODEL = "llama3.2:1b"`
  - `EMBEDDING_MODEL = "nomic-embed-text"`
- **Cloud Constraint**: Ollama runs as a local background daemon on laptop. Standard Google Cloud Run containers do not have Ollama or local GPU execution. Needs environment variable parameterization with graceful offline handling.

### b. Local SQLite Persistence
- **Location**: `backend/database.py` lines 6–7:
  - `DEFAULT_DB_PATH = Path(__file__).resolve().parent / "tradeiqx.db"`
  - `DATABASE_URL = os.getenv("DATABASE_URL", f"sqlite:///{DEFAULT_DB_PATH}")`
- **Cloud Constraint**: Cloud Run instances have ephemeral, in-memory filesystems. Any write to SQLite will be lost when instances scale down or restart. For persistence in production, a managed database like Cloud SQL Postgres is required via `DATABASE_URL`.

### c. RAG Knowledge-Base File Path
- **Location**: `backend/rag_service.py` lines 14–19:
  - `PROJECT_DIR = BACKEND_DIR.parent`
  - `KNOWLEDGE_FILE = PROJECT_DIR / "rag" / "tradeiqx_knowledge.json"`
- **Git Tracking**: Confirmed tracked in git (`rag/tradeiqx_knowledge.json`).
- **Cloud Constraint**: If only `backend/` is copied into the Docker container, `PROJECT_DIR / "rag"` will resolve to a non-existent path. The Dockerfile or code must account for the knowledge file location inside the container image.

### d. Missing Dependencies in `backend/requirements.txt`
Existing `backend/requirements.txt` contained only:
`fastapi`, `uvicorn[standard]`, `sqlalchemy`, `pydantic`, `passlib[bcrypt]`, `httpx`, `bcrypt==4.0.1`.
**Missing Packages**:
- `growwapi` (1.5.0) &mdash; Imported in `backend/market_data.py`
- `python-dotenv` (1.2.4) &mdash; Imported in `backend/market_data.py`
- `email-validator` (2.3.0) &mdash; Required by Pydantic for `EmailStr` in `backend/schemas.py`
- `requests` (2.34.2) &mdash; Imported in `backend/rag_service.py` and `backend/main.py`

---

## 4. Android Base URLs & Network Analysis

### URLs Currently in Android Code:
1. `app/src/main/java/com/example/tradeiq_x/BackendApi.kt`:
   - `var baseUrl: String = "http://10.0.2.2:8001"`
2. `app/src/main/java/com/example/tradeiq_x/AIService.kt`:
   - `private const val BASE_URL = "http://127.0.0.1:8002"`
3. `TradingApiClient.kt`:
   - *Audit finding*: `TradingApiClient.kt` does not exist in the active repository; early prototypes in commit history used `http://10.0.2.2:8000`.
4. `app/src/main/AndroidManifest.xml`:
   - Line 8: `android:usesCleartextTraffic="true"` enables unencrypted HTTP traffic app-wide.

### Port Evaluation (8000 vs 8001 vs 8002):
- **Finding**: Ports 8000, 8001, and 8002 represent the **exact same FastAPI backend application** (`backend/main.py`).
- **Cause of Split**: Different teammates ran `uvicorn` on different ports during development, leading to `BackendApi` pointing to port 8001 and `AIService` pointing to port 8002.
- **Production Strategy**: Both `BackendApi` and `AIService` must resolve to a single unified `BACKEND_BASE_URL` in release builds (e.g. `https://tradeiqx-backend-xxxx.run.app` or Firebase Hosting rewrite `https://tradeiqx-app.web.app/api`).
- **Cleartext Security**: Remove global `usesCleartextTraffic="true"` from release builds, restricting cleartext HTTP to debug builds via Android network security configuration for `10.0.2.2` and `localhost`.
