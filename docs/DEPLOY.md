# TradeIQX Cloud Run & Firebase Deployment Guide

> **Status**: Prepared & Verified Locally.  
> **Safety Notice**: No cloud resources have been created, no deployment commands have been executed, and no accounts have been authenticated. This guide outlines the exact, step-by-step procedure for manual deployment.

---

## Part A: What Was Prepared Automatically

The following configuration and deployment artifacts were generated in the repository:

1. **[`backend/requirements.txt`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/requirements.txt)**: Added all missing runtime dependencies (`growwapi`, `python-dotenv`, `email-validator`, `requests`) with verified compatible versions.
2. **[`backend/Dockerfile`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/Dockerfile)**: Multi-stage, non-root user (`appuser`), slim Python 3.11 container listening dynamically on `$PORT` (default 8080) for Cloud Run.
3. **[`backend/.dockerignore`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/.dockerignore)**: Excludes `.env`, `.venv`, `__pycache__`, local database files (`*.db`), unit tests, and git metadata.
4. **[`backend/main.py`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/main.py)**: Added CORS middleware with customizable `ALLOWED_ORIGINS`, decoupled `/health` check from database dependencies, added `/ready` database check, and ensured safe SQLite dialect guards for Cloud SQL compatibility.
5. **[`backend/rag_service.py`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/rag_service.py)**: Parameterized `OLLAMA_URL`, `CHAT_MODEL`, and `EMBEDDING_MODEL` through environment variables with zero changes to current defaults.
6. **[`backend/rag/tradeiqx_knowledge.json`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/backend/rag/tradeiqx_knowledge.json)**: Bundled financial knowledge base directly inside backend image for self-contained container builds.
7. **[`app/build.gradle.kts`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/build.gradle.kts)**: Enabled `BuildConfig`, configured `BACKEND_BASE_URL` to read from Gradle property or `local.properties` for release builds, while leaving debug builds untouched.
8. **[`app/src/main/AndroidManifest.xml`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/main/AndroidManifest.xml)** & **[`app/src/debug/AndroidManifest.xml`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/app/src/debug/AndroidManifest.xml)**: Removed global `usesCleartextTraffic="true"`; cleartext is now isolated strictly to debug builds. Release builds mandate HTTPS.
9. **[`firebase.json`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/firebase.json)** & **[`.firebaserc.example`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/.firebaserc.example)**: Hosting rewrite configuration routing `/api/**` and root traffic to Cloud Run service `tradeiqx-backend`.
10. **[`.gitignore`](file:///c:/Users/Navya/AndroidStudioProjects/TradeIQX/.gitignore)**: Enforced exclusion of `.firebase/`, secrets, service account credentials, and database files.

---

## Part B: Manual Cloud Deployment Steps (What Only You Can Do)

### Step 1: Authentication & Project Selection
In your local terminal:

```bash
# Authenticate with Google Cloud SDK
gcloud auth login

# Authenticate with Firebase CLI
firebase login

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
    secretmanager.googleapis.com \
    sqladmin.googleapis.com
```

### Step 3: Configure Cloud Secrets in Secret Manager
Never commit secrets to git or `.env` files. Store them securely in Google Secret Manager:

```bash
# 1. Groww API credentials
echo -n "YOUR_ACTUAL_GROWW_API_KEY" | gcloud secrets create GROWW_API_KEY --data-file=-
echo -n "YOUR_ACTUAL_GROWW_API_SECRET" | gcloud secrets create GROWW_API_SECRET --data-file=-

# 2. Database Connection URL (e.g., Cloud SQL PostgreSQL or Neon/Supabase)
# Format: postgresql+psycopg2://USER:PASSWORD@HOST:5432/DBNAME
echo -n "YOUR_DATABASE_URL" | gcloud secrets create DATABASE_URL --data-file=-

# 3. AI Provider Key (if Vertex AI / Gemini API is chosen)
# echo -n "YOUR_GEMINI_API_KEY" | gcloud secrets create GEMINI_API_KEY --data-file=-
```

### Step 4: Grant Cloud Run Access to Secrets
Find your project's default Compute Engine service account (or dedicated Cloud Run service account):

```bash
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
SERVICE_ACCOUNT="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

# Grant Secret Accessor role
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/secretmanager.secretAccessor"
```

### Step 5: Deploy Backend to Google Cloud Run
From the `backend/` directory:

```bash
cd backend

gcloud run deploy tradeiqx-backend \
    --source . \
    --region asia-south1 \
    --platform managed \
    --allow-unauthenticated \
    --set-secrets="GROWW_API_KEY=GROWW_API_KEY:latest,GROWW_API_SECRET=GROWW_API_SECRET:latest,DATABASE_URL=DATABASE_URL:latest" \
    --set-env-vars="ALLOWED_ORIGINS=*" \
    --memory 512Mi \
    --cpu 1 \
    --min-instances 0 \
    --max-instances 5
```

Note the output **Service URL** (e.g., `https://tradeiqx-backend-xyz-el.a.run.app`).

### Step 6: Deploy Firebase Hosting Gateway (Optional)
If you want clean URL rewrites or web landing page hosting:

```bash
# In the project root
cp .firebaserc.example .firebaserc
# Edit .firebaserc to replace "your-firebase-project-id" with your actual $PROJECT_ID

firebase deploy --only hosting
```

### Step 7: Configure Android Release Base URL
Add the deployment URL to your private `local.properties` (which is git-ignored):

```properties
# In local.properties:
tradeiqx.backend.url=https://tradeiqx-backend-xyz-el.a.run.app
```

Or pass via command line when assembling the release APK:
```powershell
.\gradlew.bat assembleRelease -PTRADEIQX_BACKEND_URL="https://tradeiqx-backend-xyz-el.a.run.app"
```

---

## Part C: Post-Deployment Verification Checklist

Replace `https://YOUR_BACKEND_URL` with your actual Cloud Run URL:

| Test # | Endpoint / Action | Exact cURL Command | Expected Result |
|---|---|---|---|
| 1 | Liveness Health Check | `curl -f -s https://YOUR_BACKEND_URL/health` | HTTP 200 `{"status":"ok"}` |
| 2 | Database Readiness | `curl -f -s https://YOUR_BACKEND_URL/ready` | HTTP 200 `{"status":"ready","database":"healthy"}` |
| 3 | User Signup | `curl -s -X POST https://YOUR_BACKEND_URL/auth/signup -H "Content-Type: application/json" -d '{"username":"clouduser","email":"cloud@test.com","password":"Password123"}'` | HTTP 201 `{"message":"User created successfully",...}` |
| 4 | User Login | `curl -s -X POST https://YOUR_BACKEND_URL/auth/login -H "Content-Type: application/json" -d '{"email":"cloud@test.com","password":"Password123"}'` | HTTP 200 `{"access_token":...,"user_id":...}` |
| 5 | Get Profile (`/auth/me`) | `curl -s -X GET "https://YOUR_BACKEND_URL/auth/me?user_id=1"` | HTTP 200 `{"id":1,"username":"clouduser","cash_balance":100000.0,...}` |
| 6 | Get Portfolio | `curl -s -X GET "https://YOUR_BACKEND_URL/portfolio" -H "X-User-Id: 1"` | HTTP 200 `{"cash_balance":100000.0,"holdings":[]}` |
| 7 | Buy Stock | `curl -s -X POST https://YOUR_BACKEND_URL/buy -H "Content-Type: application/json" -d '{"symbol":"RELIANCE","shares":1,"user_id":1}'` | HTTP 200 order execution summary |
| 8 | Sell Stock | `curl -s -X POST https://YOUR_BACKEND_URL/sell -H "Content-Type: application/json" -d '{"symbol":"RELIANCE","shares":1,"user_id":1}'` | HTTP 200 order execution summary |
| 9 | Market Quote | `curl -s -X GET https://YOUR_BACKEND_URL/market/quote/RELIANCE` | HTTP 200 LTP payload or HTTP 403 `provider_unauthorised` (if Groww keys restricted) |
| 10 | AI Stock Analysis | `curl -s -X POST https://YOUR_BACKEND_URL/analyze -H "Content-Type: application/json" -d '{"symbol":"RELIANCE","signal":"BUY","risk":"LOW","indicators":"RSI 30"}'` | HTTP 200 explanation or clean 503 if AI provider offline |
| 11 | AI Mentor Chat | `curl -s -X POST https://YOUR_BACKEND_URL/api/chat -H "Content-Type: application/json" -d '{"message":"What is RSI?"}'` | HTTP 200 reply or clean 503 if AI provider offline |
| 12 | AI Quiz Generation | `curl -s -X POST https://YOUR_BACKEND_URL/api/quiz/generate -H "Content-Type: application/json" -d '{"topic":"Stock Market Basics","difficulty":"Beginner"}'` | HTTP 200 quiz structure or clean 503 if AI provider offline |
