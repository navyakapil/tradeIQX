from datetime import datetime
from typing import Optional
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
    cash_balance: float = 100000.0

    class Config:
        from_attributes = True


class TradeRequest(BaseModel):
    symbol: str = Field(min_length=1, max_length=20)
    quantity: int = Field(gt=0)
    price: float = Field(gt=0)
