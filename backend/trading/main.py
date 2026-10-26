"""TradeIQX Trading Engine - FastAPI Service.

Provides server-authoritative paper-trading execution, portfolio valuation,
and persistent order/trade history.
"""

from typing import List
from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from database import engine, Base, get_db
from models import Account, Holding
from prices import get_stock_info, get_server_price
from schemas import (
    QuoteResponse,
    OrderCreate,
    OrderResponse,
    PortfolioSummary,
    HoldingResponse,
)
from deps import get_current_account
from services import (
    execute_buy_order,
    execute_sell_order,
    get_existing_idempotent_order,
)

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
    """Places an order with optional idempotency protection against duplicate retries."""
    if payload.idempotency_key:
        existing = get_existing_idempotent_order(
            db=db,
            user_id=account.user_id,
            idempotency_key=payload.idempotency_key,
        )
        if existing:
            return existing

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


@app.get("/portfolio", response_model=PortfolioSummary)
def get_portfolio(
    account: Account = Depends(get_current_account),
    db: Session = Depends(get_db),
):
    """Calculates user portfolio valuation, invested capital, and comprehensive P&L."""
    active_holdings = (
        db.query(Holding)
        .filter(Holding.user_id == account.user_id, Holding.quantity > 0)
        .order_by(Holding.symbol.asc())
        .all()
    )

    holding_views: List[HoldingResponse] = []
    total_invested: float = 0.0
    total_current_value: float = 0.0

    for h in active_holdings:
        try:
            curr_price = get_server_price(h.symbol)
        except HTTPException:
            curr_price = h.avg_buy_price

        invested = round(h.avg_buy_price * h.quantity, 2)
        curr_val = round(curr_price * h.quantity, 2)
        unrealized = round(curr_val - invested, 2)
        unrealized_pct = round((unrealized / invested * 100.0), 2) if invested > 0 else 0.0

        total_invested += invested
        total_current_value += curr_val

        holding_views.append(
            HoldingResponse(
                symbol=h.symbol,
                quantity=h.quantity,
                avg_buy_price=h.avg_buy_price,
                current_price=curr_price,
                invested_value=invested,
                current_value=curr_val,
                unrealized_pnl=unrealized,
                unrealized_pnl_percent=unrealized_pct,
                realized_pnl=h.realized_pnl,
            )
        )

    total_invested = round(total_invested, 2)
    total_current_value = round(total_current_value, 2)
    portfolio_value = round(account.cash_balance + total_current_value, 2)
    unrealized_pnl = round(total_current_value - total_invested, 2)
    realized_pnl = round(account.realized_pnl, 2)
    total_pnl = round(unrealized_pnl + realized_pnl, 2)
    return_pct = round((total_pnl / total_invested * 100.0), 2) if total_invested > 0 else 0.0

    return PortfolioSummary(
        user_id=account.user_id,
        cash_balance=round(account.cash_balance, 2),
        total_invested=total_invested,
        current_value=total_current_value,
        portfolio_value=portfolio_value,
        unrealized_pnl=unrealized_pnl,
        realized_pnl=realized_pnl,
        total_pnl=total_pnl,
        return_percent=return_pct,
        holdings=holding_views,
    )
