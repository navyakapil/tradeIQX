"""TradeIQX Trading Engine - FastAPI Service.

Provides server-authoritative paper-trading execution, portfolio valuation,
and persistent order/trade history.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from database import engine, Base
from prices import get_stock_info
from schemas import QuoteResponse

# Initialize database schema on startup
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="TradeIQX Trading Engine",
    description="Paper trading backend service for order execution and persistent portfolios",
    version="1.0.0",
)

# Enable CORS for cross-origin and emulator access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health_check():
    """Health check endpoint to verify trading engine status."""
    return {
        "status": "healthy",
        "service": "trading-engine",
    }


@app.get("/quote/{symbol}", response_model=QuoteResponse)
def get_quote(symbol: str):
    """Returns server-authoritative price quote for a symbol."""
    info = get_stock_info(symbol)
    return QuoteResponse(
        symbol=info["symbol"],
        name=info["name"],
        price=info["price"],
        currency="INR",
    )
