# TradeIQX Cloud Run & Render Deployment Guide

> **Status**: Prepared & Verified Locally.  
> **Safety Notice**: No cloud resources have been created, no deployment commands have been executed, and no accounts have been authenticated. This guide outlines the exact, step-by-step procedure for manual deployment.

---

## Part A: What Was Prepared Automatically

The following configuration and deployment artifacts were generated in the repository:

1. **[`backend/requirements.txt`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/requirements.txt)**: Added all missing runtime dependencies (`growwapi`, `python-dotenv`, `email-validator`, `requests`, `psycopg2-binary`) with verified compatible versions.
2. **[`backend/Dockerfile`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/Dockerfile)**: Multi-stage, non-root user (`appuser`), slim Python 3.11 container listening dynamically on `$PORT` (default 8080) for Cloud Run / Render.
3. **[`backend/.dockerignore`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/.dockerignore)**: Excludes `.env`, `.venv`, `__pycache__`, local database files (`*.db`), unit tests, and git metadata.
4. **[`backend/main.py`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/main.py)**: Added CORS middleware with customizable `ALLOWED_ORIGINS`, decoupled `/health` check from database dependencies, added `/ready` database check, and integrated Person 3's Trading Engine routes (`/quote`, `/orders`, `/transactions`, `/portfolio`) into one unified service.
5. **[`backend/rag_service.py`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/rag_service.py)**: Parameterized `OLLAMA_URL`, `CHAT_MODEL`, and `EMBEDDING_MODEL` through environment variables with zero changes to current defaults.
6. **[`backend/rag/tradeiqx_knowledge.json`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/rag/tradeiqx_knowledge.json)**: Bundled financial knowledge base directly inside backend image for self-contained container builds.
7. **[`app/build.gradle.kts`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/build.gradle.kts)**: Enabled `BuildConfig`, configured `BACKEND_BASE_URL` to read from Gradle property or `local.properties` for both physical phone debugging and release builds.
8. **[`app/src/main/AndroidManifest.xml`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/AndroidManifest.xml)** & **[`app/src/debug/AndroidManifest.xml`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/debug/AndroidManifest.xml)**: Removed global `usesCleartextTraffic="true"`; cleartext is now isolated strictly to debug builds. Release builds mandate HTTPS.
9. **[`firebase.json`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/firebase.json)** & **[`.firebaserc.example`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/.firebaserc.example)**: Hosting rewrite configuration routing `/api/**` and root traffic to Cloud Run service `tradeiqx-backend`.
10. **[`.gitignore`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/.gitignore)**: Enforced exclusion of `.firebase/`, secrets, service account credentials, and database files.

---

## Part B: Option 1 — Cardless Free Deployment (Render.com)

If you prefer **no credit card required ($0/month)**:

1. **Sign up**: Go to [dashboard.render.com](https://dashboard.render.com/) and sign up with your GitHub account.
2. **Create Web Service**:
   - Click **New +** -> **Web Service**.
   - Select your repository `TradeIQX`.
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Runtime**: `Python 3`
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - **Plan**: `Free` ($0/mo)
3. **Environment Variables**:
   Under the **Environment** tab on Render, add:
   - `ALLOWED_ORIGINS` = `*`
   - `GROWW_API_KEY` = *(your groww key)*
   - `GROWW_API_SECRET` = *(your groww secret)*
4. **Deploy**:
   - Click **Deploy Web Service**.
   - Render will build and deploy in ~2 minutes and provide a live URL:  
     `https://tradeiqx-backend.onrender.com`

---

## Part C: Option 2 — Google Cloud Run Deployment

### Step 1: Authentication & Project Selection
In your local terminal:

```bash
# Authenticate with Google Cloud SDK
gcloud auth login

# Set your active Google Cloud project ID
export PROJECT_ID="your-project-id"
gcloud config set project $PROJECT_ID
```

### Step 2: Enable Required Google Cloud APIs
```bash
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    cloudbuild.googleapis.com \
    secretmanager.googleapis.com
```

### Step 3: Configure Cloud Secrets in Secret Manager
Never commit secrets to git or `.env` files. Store them securely in Google Secret Manager:

```bash
echo -n "YOUR_ACTUAL_GROWW_API_KEY" | gcloud secrets create GROWW_API_KEY --data-file=-
echo -n "YOUR_ACTUAL_GROWW_API_SECRET" | gcloud secrets create GROWW_API_SECRET --data-file=-
```

### Step 4: Deploy Backend to Google Cloud Run
From the `backend/` directory:

```bash
cd backend

gcloud run deploy tradeiqx-backend \
    --source . \
    --region asia-south1 \
    --platform managed \
    --allow-unauthenticated \
    --set-secrets="GROWW_API_KEY=GROWW_API_KEY:latest,GROWW_API_SECRET=GROWW_API_SECRET:latest" \
    --set-env-vars="ALLOWED_ORIGINS=*" \
    --memory 512Mi \
    --cpu 1 \
    --min-instances 0 \
    --max-instances 5
```

---

## Part D: Connect Your Android App (Mobile Phone & Emulator)

Add your deployed cloud URL to `local.properties` (this file is git-ignored and stays strictly on your computer):

```properties
tradeiqx.backend.url=https://YOUR_DEPLOYED_URL
```

Then build and install the APK on your phone:
```powershell
.\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

All three Android clients (`TradingApiClient`, `BackendApi`, `AIService`) automatically route over HTTPS to your unified cloud backend!

---

## Part E: Post-Deployment Verification Checklist

Replace `https://YOUR_BACKEND_URL` with your actual live URL:

| Test # | Endpoint / Action | Exact cURL Command | Expected Result |
|---|---|---|---|
| 1 | Liveness Health Check | `curl -f -s https://YOUR_BACKEND_URL/health` | HTTP 200 `{"status":"ok"}` |
| 2 | Database Readiness | `curl -f -s https://YOUR_BACKEND_URL/ready` | HTTP 200 `{"status":"ready","database":"healthy"}` |
| 3 | Paper Trading Quote | `curl -s https://YOUR_BACKEND_URL/quote/RELIANCE` | HTTP 200 `{"symbol":"RELIANCE","price":1450.25,...}` |
| 4 | Submit Paper Order | `curl -s -X POST https://YOUR_BACKEND_URL/orders -H "Content-Type: application/json" -H "X-User-Id: mobile_user" -d "{\"symbol\":\"RELIANCE\",\"side\":\"BUY\",\"quantity\":5}"` | HTTP 200 `{"id":"ord_...","status":"COMPLETED",...}` |
| 5 | Mobile Portfolio Summary | `curl -s https://YOUR_BACKEND_URL/portfolio -H "X-User-Id: mobile_user"` | HTTP 200 `PortfolioSummary` with cash, P&L, holdings |
| 6 | Transaction History | `curl -s https://YOUR_BACKEND_URL/transactions -H "X-User-Id: mobile_user"` | HTTP 200 list of executed trades |
| 7 | User Signup | `curl -s -X POST https://YOUR_BACKEND_URL/auth/signup -H "Content-Type: application/json" -d "{\"username\":\"clouduser\",\"email\":\"cloud@test.com\",\"password\":\"Password123\"}"` | HTTP 201 `{"id":...,"username":"clouduser"}` |
| 8 | User Login | `curl -s -X POST https://YOUR_BACKEND_URL/auth/login -H "Content-Type: application/json" -d "{\"email\":\"cloud@test.com\",\"password\":\"Password123\"}"` | HTTP 200 `{"user_id":...,"username":"clouduser"}` |
| 9 | Get Profile (`/auth/me`) | `curl -s -X GET "https://YOUR_BACKEND_URL/auth/me?user_id=1"` | HTTP 200 profile with email and balance |
| 10 | Groww Market Quote | `curl -s -X GET https://YOUR_BACKEND_URL/market/quote/RELIANCE` | HTTP 200 LTP payload or 403 provider_unauthorised |
| 11 | AI Stock Analysis | `curl -s -X POST https://YOUR_BACKEND_URL/analyze -H "Content-Type: application/json" -d "{\"symbol\":\"RELIANCE\",\"signal\":\"BUY\",\"risk\":\"LOW\",\"indicators\":\"RSI 30\"}"` | HTTP 200 explanation |
| 12 | AI Mentor Chat | `curl -s -X POST https://YOUR_BACKEND_URL/api/chat -H "Content-Type: application/json" -d "{\"message\":\"What is RSI?\"}"` | HTTP 200 reply or clean 503 if Ollama offline |
| 13 | AI Quiz Generation | `curl -s -X POST https://YOUR_BACKEND_URL/api/quiz/generate -H "Content-Type: application/json" -d "{\"topic\":\"Stock Market Basics\",\"difficulty\":\"Beginner\"}"` | HTTP 200 quiz structure or clean 503 if Ollama offline |
