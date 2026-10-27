"""FastAPI dependencies and user bootstrapping for TradeIQX Trading Engine.

Extracts the authenticated or device-scoped X-User-Id header and ensures an Account
record is automatically provisioned with the initial virtual starting cash.
"""

from fastapi import Header, Depends, HTTPException
from sqlalchemy.orm import Session
try:
    from .database import get_db
    from .models import Account, DEFAULT_STARTING_CASH
except (ImportError, ValueError):
    from database import get_db
    from models import Account, DEFAULT_STARTING_CASH



def get_current_user_id(
    x_user_id: str = Header(
        default="default_user",
        alias="X-User-Id",
        description="Unique user or Android device identifier",
    )
) -> str:
    """Extracts and normalizes the X-User-Id header."""
    normalized = x_user_id.strip()
    if not normalized:
        raise HTTPException(status_code=400, detail="X-User-Id header cannot be empty")
    return normalized


def get_or_create_account(
    db: Session,
    user_id: str,
) -> Account:
    """Retrieves an existing Account or auto-provisions a new one with starting cash."""
    account = db.query(Account).filter(Account.user_id == user_id).first()
    if not account:
        account = Account(
            user_id=user_id,
            cash_balance=DEFAULT_STARTING_CASH,
            realized_pnl=0.0,
        )
        db.add(account)
        db.commit()
        db.refresh(account)
    return account


def get_current_account(
    user_id: str = Depends(get_current_user_id),
    db: Session = Depends(get_db),
) -> Account:
    """Dependency that resolves the Account entity for the current request user."""
    return get_or_create_account(db, user_id)
