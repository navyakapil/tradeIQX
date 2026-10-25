
import logging

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from rag_service import answer_question


# ---------------------------------
# CONFIGURATION
# ---------------------------------

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="TradeIQX API",
    description="Paper trading and AI investment learning platform",
    version="1.0.0"
)


# ---------------------------------
# HOME API
# ---------------------------------

@app.get("/")
def home():
    return {
        "message": "TradeIQX backend is running!",
        "status": "online"
    }


# ---------------------------------
# HEALTH CHECK
# ---------------------------------

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "chatbot_mode": "local_rag",
        "llm_provider": "ollama",
        "llm_model": "llama3.2:1b",
        "rag_enabled": True
    }


# ---------------------------------
# STOCK ANALYSIS REQUEST MODEL
# ---------------------------------

class StockAnalysisRequest(BaseModel):
    symbol: str = Field(min_length=1)
    signal: str = Field(min_length=1)
    risk: str = Field(min_length=1)
    indicators: str = Field(min_length=1)


# ---------------------------------
# SIMULATED STOCK ANALYSIS
# ---------------------------------

@app.post("/analyze")
def analyze_stock(data: StockAnalysisRequest):

    explanation = (
        f"{data.symbol.upper()} has a simulated "
        f"{data.signal} signal with a {data.risk} "
        f"risk indicator. Technical analysis: "
        f"{data.indicators}. "
        "This is educational information, "
        "not financial advice."
    )

    return {
        "symbol": data.symbol.upper(),
        "signal": data.signal,
        "risk": data.risk,
        "explanation": explanation,
        "source": "fallback"
    }


# ---------------------------------
# AI MENTOR REQUEST MODEL
# ---------------------------------

class ChatRequest(BaseModel):
    message: str = Field(min_length=1)


# ---------------------------------
# RAG-BASED AI TRADING MENTOR
# ---------------------------------

@app.post("/api/chat")
def chat(data: ChatRequest):

    question = data.message.strip()

    if not question:
        raise HTTPException(
            status_code=400,
            detail="Please enter a trading question."
        )

    try:
        result = answer_question(question)

        return result

    except FileNotFoundError:
        logger.exception("RAG knowledge base missing")

        raise HTTPException(
            status_code=503,
            detail="Financial knowledge base not found."
        )

    except Exception as exc:
        logger.exception("RAG chatbot failed")

        raise HTTPException(
            status_code=503,
            detail=(
                "AI Mentor is temporarily unavailable. "
                "Check that Ollama is running and "
                "the embedding model is installed."
            )
        ) from exc
