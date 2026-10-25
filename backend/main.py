
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Dict
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Dict

from sqlalchemy.orm import Session
from database import SessionLocal, engine
from models import Base, User
import schemas

from passlib.context import CryptContext

app = FastAPI(title="TradeIQX API")
Base.metadata.create_all(bind=engine)

pwd_context = CryptContext(
    schemes=["bcrypt"],
    deprecated="auto"
)

# Temporary in-memory paper-trading data.
# We will replace this with SQL persistence in the next step.
INITIAL_BALANCE = 100000.0

cash_balance = INITIAL_BALANCE
holdings: Dict[str, int] = {}
transactions = []
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def hash_password(password: str):
    return pwd_context.hash(password)


def verify_password(plain, hashed):
    return pwd_context.verify(plain, hashed)

class TradeRequest(BaseModel):
    symbol: str = Field(min_length=1, max_length=20)
    quantity: int = Field(gt=0)
    price: float = Field(gt=0)


@app.get("/")
def home():
    return {"message": "TradeIQX backend is running!"}


@app.get("/health")
def health_check():
    return {"status": "healthy"}


@app.get("/portfolio")
def get_portfolio():
    return {
        "cash_balance": round(cash_balance, 2),
        "holdings": holdings,
        "transactions": transactions,
    }


@app.post("/buy")
def buy_stock(trade: TradeRequest):
    global cash_balance

    symbol = trade.symbol.strip().upper()
    if not symbol:
        raise HTTPException(status_code=400, detail="Stock symbol is required")

    total_cost = trade.quantity * trade.price

    if total_cost > cash_balance:
        raise HTTPException(status_code=400, detail="Insufficient virtual cash")

    cash_balance = round(cash_balance - total_cost, 2)
    holdings[symbol] = holdings.get(symbol, 0) + trade.quantity

    transaction = {
        "symbol": symbol,
        "side": "BUY",
        "quantity": trade.quantity,
        "price": trade.price,
        "total": round(total_cost, 2),
    }
    transactions.append(transaction)

    return {
        "message": "Paper trade completed",
        "transaction": transaction,
        "portfolio": get_portfolio(),
    }


@app.post("/sell")
def sell_stock(trade: TradeRequest):
    global cash_balance

    symbol = trade.symbol.strip().upper()
    current_quantity = holdings.get(symbol, 0)

    if current_quantity < trade.quantity:
        raise HTTPException(
            status_code=400,
            detail="Insufficient shares to sell",
        )

    total_value = trade.quantity * trade.price

    cash_balance = round(cash_balance + total_value, 2)
    remaining = current_quantity - trade.quantity

    if remaining == 0:
        del holdings[symbol]
    else:
        holdings[symbol] = remaining

    transaction = {
        "symbol": symbol,
        "side": "SELL",
        "quantity": trade.quantity,
        "price": trade.price,
        "total": round(total_value, 2),
    }
    transactions.append(transaction)

    return {
        "message": "Paper trade completed",
        "transaction": transaction,
        "portfolio": get_portfolio(),
    }