"""Pydantic validation schemas for TradeIQX Trading Engine.

Defines request and response shapes for order submissions, active holdings,
portfolio summaries, and executed trade transactions.
"""

from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field


class OrderCreate(BaseModel):
    """Payload to place a market buy or sell order."""
    symbol: str = Field(..., min_length=1, max_length=32, description="Stock ticker symbol")
    side: str = Field(..., pattern=r"^(BUY|SELL)$", description="Order direction: BUY or SELL")
    quantity: int = Field(..., gt=0, description="Number of whole shares to trade")
    idempotency_key: Optional[str] = Field(
        None,
        max_length=128,
        description="Optional unique key to prevent duplicate order executions",
    )


class OrderResponse(BaseModel):
    """Representation of an order status after placement."""
    id: str
    user_id: str
    symbol: str
    side: str
    quantity: int
    price: float
    total_value: float
    status: str
    idempotency_key: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True


class HoldingResponse(BaseModel):
    """Detailed holding view with purchase metrics and unrealized P&L."""
    symbol: str
    quantity: int
    avg_buy_price: float
    current_price: float
    invested_value: float
    current_value: float
    unrealized_pnl: float
    unrealized_pnl_percent: float
    realized_pnl: float

    class Config:
        from_attributes = True


class PortfolioSummary(BaseModel):
    """Comprehensive portfolio analytics, cash balances, and holding breakdowns."""
    user_id: str
    cash_balance: float
    total_invested: float
    current_value: float
    portfolio_value: float
    unrealized_pnl: float
    realized_pnl: float
    total_pnl: float
    return_percent: float
    holdings: List[HoldingResponse]

    class Config:
        from_attributes = True


class TradeResponse(BaseModel):
    """Executed trade transaction item for audit and history."""
    id: int
    order_id: str
    user_id: str
    symbol: str
    side: str
    quantity: int
    price: float
    total_value: float
    executed_at: datetime

    class Config:
        from_attributes = True


class QuoteResponse(BaseModel):
    """Server-authoritative stock quote."""
    symbol: str
    name: str
    price: float
    currency: str = "INR"
