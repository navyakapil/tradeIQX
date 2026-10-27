"""Database layer for the TradeIQX Trading Engine.

Manages SQLite connection pooling and scoped session lifecycle.
"""

import os
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

# Place trading.db in the backend/trading directory by default
_DB_DIR = os.path.dirname(os.path.abspath(__file__))
_DB_PATH = os.path.join(_DB_DIR, "trading.db")

DATABASE_URL = os.getenv("TRADING_DATABASE_URL") or os.getenv("DATABASE_URL") or f"sqlite:///{_DB_PATH}"
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {},
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


def get_db():
    """FastAPI dependency that provides a transactional database session."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
