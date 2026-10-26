"""TradeIQX Trading Engine - FastAPI Service.

Provides server-authoritative paper-trading execution, portfolio valuation,
and persistent order/trade history.
"""

from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from database import engine, Base, get_db
from models import Account
from prices import get_stock_info
from schemas import QuoteResponse, OrderCreate, OrderResponse
from deps import get_current_account
from services import execute_buy_order, execute_sell_order

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


@app.post("/orders", response_model=OrderResponse)
def place_order(
    payload: OrderCreate,
    account: Account = Depends(get_current_account),
    db: Session = Depends(get_db),
):
    """Places an order (BUY or SELL) with atomic execution and server price authority."""
    if payload.side == "BUY":
        return execute_buy_order(
            db=db,
            account=account,
            symbol=payload.symbol,
            quantity=payload.quantity,
            idempotency_key=payload.idempotency_key,
        )
    elif payload.side == "SELL":
        return execute_sell_order(
            db=db,
            account=account,
            symbol=payload.symbol,
            quantity=payload.quantity,
            idempotency_key=payload.idempotency_key,
        )
    raise HTTPException(
        status_code=400,
        detail=f"Order side '{payload.side}' is invalid. Must be BUY or SELL.",
    )
