from pydantic import BaseModel, EmailStr


class UserRegister(BaseModel):
    username: str
    email: EmailStr
    password: str


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class UserResponse(BaseModel):
    id: int
    username: str
    email: EmailStr

    class Config:
        from_attributes = True

class TradeRequest(BaseModel):
        symbol: str
        quantity: int
        price: float

from typing import List, Optional

class MarketQuoteResponse(BaseModel):
    symbol: str
    price: float
    currency: str = 'INR'
    source: str
    fetched_at: str
    freshness: str
    previous_close: Optional[float] = None
    change: Optional[float] = None
    change_percent: Optional[float] = None
    status: str = 'LIVE'

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
    status: str = 'LIVE'
