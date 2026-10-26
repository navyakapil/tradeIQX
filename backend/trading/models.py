"""SQLAlchemy database models for TradeIQX Trading Engine.

Defines schemas for persistent accounts, active holdings, submitted orders,
and executed trades scoped per user.
"""

from datetime import datetime
from sqlalchemy import (
    Column,
    Integer,
    Float,
    String,
    DateTime,
    ForeignKey,
    Index,
)
from sqlalchemy.orm import relationship
from database import Base

DEFAULT_STARTING_CASH: float = 1_000_000.0  # ₹10,00,000


class Account(Base):
    """User account tracking paper-trading cash balance and cumulative realized P&L."""
    __tablename__ = "trading_accounts"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id = Column(String(128), unique=True, nullable=False, index=True)
    cash_balance = Column(Float, nullable=False, default=DEFAULT_STARTING_CASH)
    realized_pnl = Column(Float, nullable=False, default=0.0)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    holdings = relationship("Holding", back_populates="account", cascade="all, delete-orphan")
    orders = relationship("Order", back_populates="account", cascade="all, delete-orphan")
    trades = relationship("Trade", back_populates="account", cascade="all, delete-orphan")


class Holding(Base):
    """Active share holdings for a user in a specific stock symbol."""
    __tablename__ = "trading_holdings"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id = Column(String(128), ForeignKey("trading_accounts.user_id"), nullable=False, index=True)
    symbol = Column(String(32), nullable=False, index=True)
    quantity = Column(Integer, nullable=False, default=0)
    avg_buy_price = Column(Float, nullable=False, default=0.0)
    realized_pnl = Column(Float, nullable=False, default=0.0)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    account = relationship("Account", back_populates="holdings")

    __table_args__ = (
        Index("idx_user_symbol", "user_id", "symbol", unique=True),
    )


class Order(Base):
    """Submitted buy/sell orders with lifecycle state and idempotency tracking."""
    __tablename__ = "trading_orders"

    id = Column(String(64), primary_key=True, index=True)
    user_id = Column(String(128), ForeignKey("trading_accounts.user_id"), nullable=False, index=True)
    symbol = Column(String(32), nullable=False, index=True)
    side = Column(String(8), nullable=False)  # BUY or SELL
    quantity = Column(Integer, nullable=False)
    price = Column(Float, nullable=False)
    total_value = Column(Float, nullable=False)
    status = Column(String(16), nullable=False, default="COMPLETED")  # COMPLETED, REJECTED, FAILED
    idempotency_key = Column(String(128), nullable=True, index=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    account = relationship("Account", back_populates="orders")
    trade = relationship("Trade", back_populates="order", uselist=False)


class Trade(Base):
    """Executed trade transactions linked to orders, forming the transaction history."""
    __tablename__ = "trading_trades"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    order_id = Column(String(64), ForeignKey("trading_orders.id"), nullable=False, unique=True, index=True)
    user_id = Column(String(128), ForeignKey("trading_accounts.user_id"), nullable=False, index=True)
    symbol = Column(String(32), nullable=False, index=True)
    side = Column(String(8), nullable=False)  # BUY or SELL
    quantity = Column(Integer, nullable=False)
    price = Column(Float, nullable=False)
    total_value = Column(Float, nullable=False)
    executed_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)

    account = relationship("Account", back_populates="trades")
    order = relationship("Order", back_populates="trade")
