from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, EmailStr, Field


class UserRegister(BaseModel):
    username: str = Field(
        ...,
        min_length=3,
        max_length=50,
        pattern=r"^[a-zA-Z0-9_-]+$",
        description="Username must be 3-50 characters (letters, numbers, underscores, hyphens)",
    )
    email: EmailStr
    password: str = Field(
        ...,
        min_length=8,
        max_length=128,
        description="Password must be between 8 and 128 characters",
    )


class UserLogin(BaseModel):
    email: EmailStr
    password: str = Field(
        ...,
        min_length=1,
        max_length=128,
        description="Password cannot be empty",
    )


class UserResponse(BaseModel):
    id: int
    username: str
    email: EmailStr
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class LoginResponse(BaseModel):
    message: str
    user_id: int
    username: str
    email: EmailStr
    token: Optional[str] = None


class UserProfile(BaseModel):
    id: int
    username: str
    email: EmailStr
    created_at: Optional[datetime] = None
    cash_balance: float = 1_000_000.0

    class Config:
        from_attributes = True


class TradeRequest(BaseModel):
    symbol: str = Field(min_length=1, max_length=20)
    quantity: int = Field(gt=0)
    price: float = Field(gt=0)
    client_order_id: Optional[str] = None


class TradeOrderResponse(BaseModel):
    message: str
    order_id: Optional[int] = None
    transaction: Dict[str, Any]
    portfolio: Dict[str, Any]


class HoldingDetail(BaseModel):
    symbol: str
    quantity: int
    avg_price: float
    invested: float
    current_price: float
    current_value: float
    unrealized_pnl: float
    return_pct: float


class PortfolioSummaryResponse(BaseModel):
    cash: float
    holdings: List[HoldingDetail]
    total_invested: float
    portfolio_value: float
    realized_pnl: float
    unrealized_pnl: float
    total_pnl: float
    return_pct: float


class MarketQuoteResponse(BaseModel):
    symbol: str
    price: float
    currency: str = "INR"
    source: str
    fetched_at: str
    freshness: str
    previous_close: Optional[float] = None
    change: Optional[float] = None
    change_percent: Optional[float] = None
    status: str = "LIVE"


class CandleResponse(BaseModel):
    timestamp: int
    open: float
    high: float
    low: float
    close: float
    volume: int = 0


class MarketHistoryResponse(BaseModel):
    symbol: str
    range: str
    candles: List[CandleResponse]
    status: str = "LIVE"
