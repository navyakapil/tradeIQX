from typing import Dict, Optional
import math
from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from sqlalchemy.exc import SQLAlchemyError
from market_data import (
    get_live_quote,
    get_stock_history,
    MarketProviderAuthorisationError,
    MarketProviderRateLimitError,
    MarketProviderUnavailableError,
)
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
from sqlalchemy import or_, text, func
from sqlalchemy.exc import IntegrityError
from passlib.context import CryptContext

from database import SessionLocal, engine
from models import Base, User, Account, Portfolio, Holding, Order, Trade
import schemas

TradeRequest = schemas.TradeRequest

app = FastAPI(title="TradeIQX API")
Base.metadata.create_all(bind=engine)


@app.exception_handler(SQLAlchemyError)
def sqlalchemy_exception_handler(request, exc: SQLAlchemyError):
    return JSONResponse(
        status_code=500,
        content={"detail": "A database error occurred. Please try again later."}
    )


@app.exception_handler(RequestValidationError)
def validation_exception_handler(request, exc: RequestValidationError):
    error_messages = []
    for err in exc.errors():
        loc = err.get('loc', ())
        field = ' -> '.join(str(item) for item in loc if item != 'body')
        msg = err.get('msg', 'Invalid input')
        error_messages.append(f"{field}: {msg}" if field else msg)
    detail_str = '; '.join(error_messages) if error_messages else 'Validation failed'
    return JSONResponse(
        status_code=422,
        content={
            "detail": detail_str,
            "errors": exc.errors()
        }
    )

pwd_context = CryptContext(
    schemes=["bcrypt_sha256"],
    deprecated="auto"
)
# Pre-computed constant hash to mitigate user enumeration timing attacks
DUMMY_PASSWORD_HASH = "$bcrypt-sha256$v=2,t=2b,r=12$7eqIpWvB1w58BOmY8K/6Se$D.2mF2sZ1n9q7Yv3Xwz2x9y0"


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def get_current_user_id(x_user_id: Optional[int] = Header(None)) -> Optional[int]:
    return x_user_id


def get_or_create_user_account(db: Session, user_id: Optional[int] = None) -> tuple[int, Account, Portfolio]:
    effective_id = user_id if user_id is not None else 1
    user = db.query(User).filter(User.id == effective_id).first()
    if not user:
        user = User(
            id=effective_id,
            username=f"demo_trader_{effective_id}",
            email=f"trader_{effective_id}@tradeiqx.demo",
            hashed_password=DUMMY_PASSWORD_HASH
        )
        db.add(user)
        try:
            db.flush()
        except IntegrityError:
            db.rollback()
            user = db.query(User).filter(User.id == effective_id).first()

    account = db.query(Account).filter(Account.user_id == effective_id).first()
    portfolio = db.query(Portfolio).filter(Portfolio.user_id == effective_id).first()

    if not account:
        cash = portfolio.cash_balance if portfolio else 1_000_000.0
        account = Account(user_id=effective_id, cash=cash)
        db.add(account)

    if not portfolio:
        portfolio = Portfolio(user_id=effective_id, cash_balance=account.cash)
        db.add(portfolio)

    db.commit()
    db.refresh(account)
    db.refresh(portfolio)
    return effective_id, account, portfolio


def resolve_user_portfolio(db: Session, user_id: Optional[int] = None) -> Portfolio:
    _, _, portfolio = get_or_create_user_account(db, user_id)
    return portfolio


def hash_password(password: str):
    return pwd_context.hash(password)


def verify_password(plain, hashed):
    return pwd_context.verify(plain, hashed)


def validate_password_policy(password: str) -> None:
    if len(password) < 8:
        raise HTTPException(status_code=400, detail='Password must be at least 8 characters')
    if len(password) > 128:
        raise HTTPException(status_code=400, detail='Password must not exceed 128 characters')
    if not any(c.isdigit() for c in password):
        raise HTTPException(status_code=400, detail='Password must contain at least one number')
    if not any(c.isalpha() for c in password):
        raise HTTPException(status_code=400, detail='Password must contain at least one letter')



@app.get("/")
def home():
    return {"message": "TradeIQX backend is running!"}


@app.get("/health")
def health_check(db: Session = Depends(get_db)):
    db_status = "healthy"
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        db_status = "unhealthy"

    return {
        "status": "healthy" if db_status == "healthy" else "degraded",
        "database": db_status
    }


@app.get("/market/quote/{symbol}", response_model=schemas.MarketQuoteResponse)
def market_quote(symbol: str):
    try:
        return get_live_quote(symbol)
    except MarketProviderAuthorisationError:
        raise HTTPException(
            status_code=403,
            detail="provider_unauthorised",
        )
    except MarketProviderRateLimitError:
        raise HTTPException(
            status_code=429,
            detail="rate_limited",
        )
    except MarketProviderUnavailableError:
        raise HTTPException(
            status_code=503,
            detail="unavailable",
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )
    except LookupError:
        raise HTTPException(
            status_code=404,
            detail="Stock quote not found for this symbol",
        )
    except Exception:
        raise HTTPException(
            status_code=503,
            detail="unavailable",
        )

@app.get("/market/history/{symbol}", response_model=schemas.MarketHistoryResponse)
def market_history(symbol: str, range: str = "1D"):
    try:
        return get_stock_history(symbol=symbol, range_str=range)
    except MarketProviderAuthorisationError:
        raise HTTPException(
            status_code=403,
            detail="provider_unauthorised",
        )
    except MarketProviderRateLimitError:
        raise HTTPException(
            status_code=429,
            detail="rate_limited",
        )
    except MarketProviderUnavailableError:
        raise HTTPException(
            status_code=503,
            detail="unavailable",
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )
    except LookupError:
        raise HTTPException(
            status_code=404,
            detail="Stock quote not found for this symbol",
        )
    except Exception:
        raise HTTPException(
            status_code=503,
            detail="unavailable",
        )
@app.get("/portfolio")
def get_portfolio(
    db: Session = Depends(get_db),
    user_id: Optional[int] = Depends(get_current_user_id),
):

    portfolio = resolve_user_portfolio(db, user_id)

    if portfolio is None:
        raise HTTPException(
            status_code=404,
            detail="No portfolio found. Please create a portfolio first.",
        )

    user_holdings = (
        db.query(Holding)
        .filter(Holding.user_id == portfolio.user_id)
        .all()
    )

    total_invested = round(
        sum(
            holding.quantity * holding.average_price
            for holding in user_holdings
        ),
        2,
    )

    # Calculate current market value.
    market_value = 0.0
    prices_available = True

    for holding in user_holdings:

        if holding.quantity <= 0:
            continue

        try:
            quote = get_live_quote(holding.symbol)
            current_price = float(quote["price"])

            if (
                not math.isfinite(current_price)
                or current_price <= 0
            ):
                raise ValueError("Invalid market price")

            market_value += (
                holding.quantity * current_price
            )

        except (
            MarketProviderAuthorisationError,
            MarketProviderRateLimitError,
            MarketProviderUnavailableError,
            ValueError,
            LookupError,
            KeyError,
            TypeError,
        ):
            prices_available = False
            break

    if prices_available:

        market_value = round(market_value, 2)

        unrealized_pnl = round(
            market_value - total_invested,
            2,
        )

        unrealized_pnl_percent = (
            round(
                unrealized_pnl / total_invested * 100,
                2,
            )
            if total_invested > 0
            else 0.0
        )

        total_portfolio_value = round(
            portfolio.cash_balance + market_value,
            2,
        )

    else:
        market_value = None
        unrealized_pnl = None
        unrealized_pnl_percent = None
        total_portfolio_value = None

    user_trades = (
        db.query(Trade)
        .filter(Trade.user_id == portfolio.user_id)
        .order_by(Trade.created_at.desc())
        .all()
    )

    return {
        "cash_balance": round(
            portfolio.cash_balance, 2
        ),
        "total_invested": total_invested,
        "market_value": market_value,
        "unrealized_pnl": unrealized_pnl,
        "unrealized_pnl_percent": unrealized_pnl_percent,
        "total_portfolio_value": total_portfolio_value,
        "holdings": {
            holding.symbol: holding.quantity
            for holding in user_holdings
        },
        "average_prices": {
            holding.symbol: round(
                holding.average_price, 2
            )
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
                    if trade.created_at
                    else None
                ),
            }
            for trade in user_trades
        ],
    }


# --------------------------------------------------
# BUY STOCK
# --------------------------------------------------

@app.post("/buy")
def buy_stock(
    trade: TradeRequest,
    db: Session = Depends(get_db),
    user_id: Optional[int] = Depends(get_current_user_id)
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

    eff_user_id, account, portfolio = get_or_create_user_account(db, user_id)
    total_cost = round(trade.quantity * trade.price, 2)

    if total_cost > portfolio.cash_balance:
        raise HTTPException(
            status_code=400,
            detail="Insufficient virtual cash"
        )

    # Record order first
    client_oid = getattr(trade, "client_order_id", None)
    order = Order(
        user_id=eff_user_id,
        client_order_id=client_oid,
        side="BUY",
        symbol=symbol,
        quantity=trade.quantity,
        price=trade.price,
        status="EXECUTED"
    )
    db.add(order)
    db.flush()

    holding = (
        db.query(Holding)
        .filter(
            Holding.user_id == eff_user_id,
            Holding.symbol == symbol
        )
        .first()
    )

    if holding is None:
        holding = Holding(
            user_id=eff_user_id,
            symbol=symbol,
            quantity=trade.quantity,
            average_price=round(trade.price, 2)
        )
        db.add(holding)
    else:
        old_quantity = holding.quantity
        new_quantity = old_quantity + trade.quantity
        new_avg = round(
            ((old_quantity * holding.average_price) + (trade.quantity * trade.price)) / new_quantity,
            2
        )
        holding.quantity = new_quantity
        holding.average_price = new_avg

    # Deduct cash
    portfolio.cash_balance = round(portfolio.cash_balance - total_cost, 2)
    account.cash = portfolio.cash_balance

    # Record trade with order link and realized pnl 0.0
    transaction = Trade(
        user_id=eff_user_id,
        order_id=order.id,
        symbol=symbol,
        side="BUY",
        quantity=trade.quantity,
        price=trade.price,
        total=total_cost,
        realized_pnl=0.0
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
        .filter(Holding.user_id == eff_user_id)
        .all()
    )

    return {
        "message": "Paper trade completed",
        "order_id": order.id,
        "transaction": {
            "order_id": order.id,
            "symbol": symbol,
            "side": "BUY",
            "quantity": trade.quantity,
            "price": trade.price,
            "total": total_cost,
            "average_buy_price": holding.average_price
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
    db: Session = Depends(get_db),
    user_id: Optional[int] = Depends(get_current_user_id)
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

    eff_user_id, account, portfolio = get_or_create_user_account(db, user_id)

    holding = (
        db.query(Holding)
        .filter(
            Holding.user_id == eff_user_id,
            Holding.symbol == symbol
        )
        .first()
    )

    if holding is None or holding.quantity < trade.quantity:
        raise HTTPException(
            status_code=400,
            detail="Insufficient shares to sell"
        )

    # Calculate realized P&L based on weighted average buy price
    avg_cost = holding.average_price
    realized_pnl = round(trade.quantity * (trade.price - avg_cost), 2)
    total_value = round(trade.quantity * trade.price, 2)

    # Record order
    client_oid = getattr(trade, "client_order_id", None)
    order = Order(
        user_id=eff_user_id,
        client_order_id=client_oid,
        side="SELL",
        symbol=symbol,
        quantity=trade.quantity,
        price=trade.price,
        status="EXECUTED"
    )
    db.add(order)
    db.flush()

    holding.quantity -= trade.quantity
    remaining_qty = holding.quantity
    if holding.quantity == 0:
        db.delete(holding)

    portfolio.cash_balance = round(
        portfolio.cash_balance + total_value,
        2
    )
    account.cash = portfolio.cash_balance

    transaction = Trade(
        user_id=eff_user_id,
        order_id=order.id,
        symbol=symbol,
        side="SELL",
        quantity=trade.quantity,
        price=trade.price,
        total=total_value,
        realized_pnl=realized_pnl
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

    holdings = (
        db.query(Holding)
        .filter(Holding.user_id == eff_user_id)
        .all()
    )

    return {
        "message": "Paper trade completed",
        "order_id": order.id,
        "transaction": {
            "order_id": order.id,
            "symbol": symbol,
            "side": "SELL",
            "quantity": trade.quantity,
            "price": trade.price,
            "total": total_value,
            "realized_pnl": realized_pnl,
            "avg_buy_price": avg_cost,
            "remaining_quantity": remaining_qty
        },
        "portfolio": {
            "cash_balance": portfolio.cash_balance,
            "holdings": {
                item.symbol: item.quantity
                for item in holdings
            }
        }
    }
@app.post("/auth/signup", response_model=schemas.UserResponse, status_code=201)
def signup(user: schemas.UserRegister, db: Session = Depends(get_db)):
        username = user.username.strip()
        email = str(user.email).strip().lower()

        if not username:
            raise HTTPException(status_code=400, detail="Username is required")
        if len(username) < 3:
            raise HTTPException(status_code=400, detail="Username must be at least 3 characters")
        if len(username) > 50:
            raise HTTPException(status_code=400, detail="Username must not exceed 50 characters")

        validate_password_policy(user.password)

        existing_user = db.query(User).filter(
            or_(
                func.lower(User.email) == email,
                func.lower(User.username) == username.lower()
            )
        ).first()

        if existing_user:
            raise HTTPException(status_code=409, detail="Email or username already registered")

        new_user = User(
            username=username,
            email=email,
            hashed_password=hash_password(user.password)
        )

        try:
            db.add(new_user)
            db.flush()
            initial_portfolio = Portfolio(
                user_id=new_user.id,
                cash_balance=100000.0
            )
            db.add(initial_portfolio)
            db.commit()
            db.refresh(new_user)
        except IntegrityError:
            db.rollback()
            raise HTTPException(status_code=409, detail="Email or username already registered")
        except Exception:
            db.rollback()
            raise HTTPException(status_code=500, detail="Failed to initialize user account")

        return new_user


@app.post("/auth/login", response_model=schemas.LoginResponse)
def login(user: schemas.UserLogin, db: Session = Depends(get_db)):
        email = str(user.email).strip().lower()

        existing_user = db.query(User).filter(
            func.lower(User.email) == email
        ).first()

        if existing_user is None:
            # Perform dummy verification to equalize execution time against timing attacks
            verify_password(user.password, DUMMY_PASSWORD_HASH)
            raise HTTPException(status_code=401, detail="Invalid email or password")

        if not verify_password(user.password, existing_user.hashed_password):
            raise HTTPException(status_code=401, detail="Invalid email or password")

        return {
            "message": "Login credentials verified",
            "user_id": existing_user.id,
            "username": existing_user.username,
            "email": existing_user.email
        }


@app.get("/auth/me", response_model=schemas.UserProfile)
def get_current_user_profile(
    user_id: Optional[int] = None,
    db: Session = Depends(get_db)
):
    if user_id is not None:
        user = db.query(User).filter(User.id == user_id).first()
    else:
        user = db.query(User).order_by(User.id.asc()).first()

    if user is None:
        raise HTTPException(status_code=404, detail="User not found")

    portfolio = db.query(Portfolio).filter(Portfolio.user_id == user.id).first()
    cash_balance = portfolio.cash_balance if portfolio else 100000.0

    return schemas.UserProfile(
        id=user.id,
        username=user.username,
        email=user.email,
        created_at=user.created_at,
        cash_balance=round(cash_balance, 2)
    )

# AI Mentor imports
import logging
from rag_service import answer_question

logger = logging.getLogger(__name__)

# ---------------------------------
# RAG AI TRADING MENTOR
# ---------------------------------

class ChatRequest(BaseModel):
    question: Optional[str] = Field(None, max_length=500)
    message: Optional[str] = Field(None, max_length=500)


class ChatResponse(BaseModel):
    answer: str
    sources: list[str] = []
    reply: Optional[str] = None


@app.post("/api/chat", response_model=ChatResponse)
def chat(data: ChatRequest):
    question = (data.question or data.message or "").strip()

    if not question:
        raise HTTPException(
            status_code=422,
            detail="Question cannot be empty."
        )

    if len(question) > 500:
        raise HTTPException(
            status_code=422,
            detail="Question must not exceed 500 characters."
        )

    try:
        result = answer_question(question)
        return ChatResponse(
            answer=result["answer"],
            sources=result.get("sources", []),
            reply=result.get("reply", result["answer"])
        )

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

