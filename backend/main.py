from market_data import get_live_quote
from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel, Field
from typing import Dict
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Dict

from sqlalchemy.orm import Session
from database import SessionLocal, engine
from models import Base, User, Portfolio, Holding, Trade
import schemas

TradeRequest = schemas.TradeRequest
from passlib.context import CryptContext
from sqlalchemy import or_
from sqlalchemy.exc import IntegrityError

app = FastAPI(title="TradeIQX API")
Base.metadata.create_all(bind=engine)

pwd_context = CryptContext(
    schemes=["bcrypt_sha256"],
    deprecated="auto"
)


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
@app.get("/market/quote/{symbol}")
def market_quote(symbol: str):
    try:
        return get_live_quote(symbol)

    except ValueError as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )

    except LookupError as exc:
        raise HTTPException(
            status_code=404,
            detail=str(exc),
        )

    except RuntimeError as exc:
        raise HTTPException(
            status_code=503,
            detail=str(exc),
        )

    except Exception:
        raise HTTPException(
            status_code=502,
            detail="Market data provider request failed",
        )
@app.get("/portfolio")
def get_portfolio(db: Session = Depends(get_db)):
    portfolio = db.query(Portfolio).first()

    if portfolio is None:
        raise HTTPException(
            status_code=404,
            detail="No portfolio found. Please create a portfolio first."
        )

    user_holdings = db.query(Holding).filter(
        Holding.user_id == portfolio.user_id
    ).all()

    user_trades = (
        db.query(Trade)
        .filter(Trade.user_id == portfolio.user_id)
        .order_by(Trade.created_at.desc())
        .all()
    )

    return {
        "cash_balance": round(portfolio.cash_balance, 2),
        "holdings": {
            holding.symbol: holding.quantity
            for holding in user_holdings
        },
        "average_prices": {
            holding.symbol: round(holding.average_price, 2)
            for holding in user_holdings
        },
        "transactions": [
            {
                "symbol": trade.symbol,
                "side": trade.side,
                "quantity": trade.quantity,
                "price": trade.price,
                "total": trade.total,
                "created_at": (
                    trade.created_at.isoformat()
                    if trade.created_at else None
                ),
            }
            for trade in user_trades
        ],
    }

@app.post("/buy")
def buy_stock(
    trade: TradeRequest,
    db: Session = Depends(get_db)
):
    symbol = trade.symbol.strip().upper()

    if not symbol:
        raise HTTPException(
            status_code=400,
            detail="Stock symbol is required"
        )

    if trade.quantity <= 0:
        raise HTTPException(
            status_code=400,
            detail="Quantity must be greater than zero"
        )

    if trade.price <= 0:
        raise HTTPException(
            status_code=400,
            detail="Price must be greater than zero"
        )

    # Temporary single-user implementation.
    # We will connect this to the authenticated user next.

    portfolio = db.query(Portfolio).first()

    if portfolio is None:
        portfolio = Portfolio(
            user_id=1,
            cash_balance=100000.0
        )
        db.add(portfolio)
        db.flush()

    total_cost = round(trade.quantity * trade.price, 2)

    if total_cost > portfolio.cash_balance:
        raise HTTPException(
            status_code=400,
            detail="Insufficient virtual cash"
        )

    holding = (
        db.query(Holding)
        .filter(
            Holding.user_id == portfolio.user_id,
            Holding.symbol == symbol
        )
        .first()
    )

    if holding is None:
        holding = Holding(
            user_id=portfolio.user_id,
            symbol=symbol,
            quantity=0,
            average_price=0.0
        )
        db.add(holding)
        db.flush()

    old_quantity = holding.quantity
    new_quantity = old_quantity + trade.quantity

    holding.average_price = round(
        (
            old_quantity * holding.average_price
            + trade.quantity * trade.price
        ) / new_quantity,
        2
    )

    holding.quantity = new_quantity
    portfolio.cash_balance = round(
        portfolio.cash_balance - total_cost,
        2
    )

    transaction = Trade(
        user_id=portfolio.user_id,
        symbol=symbol,
        side="BUY",
        quantity=trade.quantity,
        price=trade.price,
        total=total_cost
    )

    db.add(transaction)

    try:
        db.commit()
        db.refresh(portfolio)
        db.refresh(holding)
    except Exception:
        db.rollback()
        raise HTTPException(
            status_code=500,
            detail="Could not complete the buy transaction"
        )

    holdings = (
        db.query(Holding)
        .filter(Holding.user_id == portfolio.user_id)
        .all()
    )

    return {
        "message": "Paper trade completed",
        "transaction": {
            "symbol": symbol,
            "side": "BUY",
            "quantity": trade.quantity,
            "price": trade.price,
            "total": total_cost
        },
        "portfolio": {
            "cash_balance": portfolio.cash_balance,
            "holdings": {
                item.symbol: item.quantity
                for item in holdings
            }
        }
    }
@app.post("/sell")
def sell_stock(
    trade: TradeRequest,
    db: Session = Depends(get_db)
):
    symbol = trade.symbol.strip().upper()

    if not symbol:
        raise HTTPException(
            status_code=400,
            detail="Stock symbol is required"
        )

    if trade.quantity <= 0:
        raise HTTPException(
            status_code=400,
            detail="Quantity must be greater than zero"
        )

    if trade.price <= 0:
        raise HTTPException(
            status_code=400,
            detail="Price must be greater than zero"
        )

    portfolio = db.query(Portfolio).first()

    if portfolio is None:
        raise HTTPException(
            status_code=400,
            detail="No portfolio found"
        )

    holding = (
        db.query(Holding)
        .filter(
            Holding.user_id == portfolio.user_id,
            Holding.symbol == symbol
        )
        .first()
    )

    if holding is None or holding.quantity < trade.quantity:
        raise HTTPException(
            status_code=400,
            detail="Insufficient shares to sell"
        )

    total_value = round(trade.quantity * trade.price, 2)

    holding.quantity -= trade.quantity

    if holding.quantity == 0:
        db.delete(holding)

    portfolio.cash_balance = round(
        portfolio.cash_balance + total_value,
        2
    )

    transaction = Trade(
        user_id=portfolio.user_id,
        symbol=symbol,
        side="SELL",
        quantity=trade.quantity,
        price=trade.price,
        total=total_value
    )

    db.add(transaction)

    try:
        db.commit()
    except Exception:
        db.rollback()
        raise HTTPException(
            status_code=500,
            detail="Could not complete the sell transaction"
        )

    return {
        "message": "Paper trade completed",
        "transaction": {
            "symbol": symbol,
            "side": "SELL",
            "quantity": trade.quantity,
            "price": trade.price,
            "total": total_value
        },
        "portfolio": get_portfolio(db)
    }
@app.post("/auth/signup", response_model=schemas.UserResponse, status_code=201)
def signup(user: schemas.UserRegister, db: Session = Depends(get_db)):
        username = user.username.strip()
        email = str(user.email).strip().lower()

        if not username:
            raise HTTPException(status_code=400, detail="Username is required")

        if len(user.password) < 8:
            raise HTTPException(status_code=400, detail="Password must be at least 8 characters")

        existing_user = db.query(User).filter(
            or_(User.email == email, User.username == username)
        ).first()

        if existing_user:
            raise HTTPException(status_code=409, detail="Email or username already registered")

        new_user = User(
            username=username,
            email=email,
            hashed_password=hash_password(user.password)
        )

        db.add(new_user)

        try:
            db.commit()
            db.refresh(new_user)
        except IntegrityError:
            db.rollback()
            raise HTTPException(status_code=409, detail="Email or username already registered")

        return new_user


@app.post("/auth/login")
def login(user: schemas.UserLogin, db: Session = Depends(get_db)):
        email = str(user.email).strip().lower()

        existing_user = db.query(User).filter(
            User.email == email
        ).first()

        if not existing_user or not verify_password(
            user.password, existing_user.hashed_password
        ):
            raise HTTPException(status_code=401, detail="Invalid email or password")

        return {
            "message": "Login credentials verified",
            "user_id": existing_user.id,
            "username": existing_user.username,
            "email": existing_user.email
        }

# AI Mentor imports
import logging
from rag_service import answer_question

logger = logging.getLogger(__name__)

# ---------------------------------
# RAG AI TRADING MENTOR
# ---------------------------------

class ChatRequest(BaseModel):
    message: str = Field(min_length=1)


@app.post("/api/chat")
def chat(data: ChatRequest):
    question = data.message.strip()

    if not question:
        raise HTTPException(
            status_code=400,
            detail="Please enter a trading question."
        )

    try:
        return answer_question(question)

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
            detail="AI Mentor is temporarily unavailable."
        ) from exc


# ---------------------------------
# SIM

# ---------------------------------
# AI STOCK ANALYSIS
# ---------------------------------

class StockAnalysisRequest(BaseModel):
    symbol: str = Field(min_length=1)
    signal: str = Field(min_length=1)
    risk: str = Field(min_length=1)
    indicators: str = Field(min_length=1)


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

