"""TradeIQX Trading Engine - FastAPI Service.

Provides server-authoritative paper-trading execution, portfolio valuation,
and persistent order/trade history.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

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
