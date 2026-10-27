"""Execution services for TradeIQX Trading Engine.

Handles atomic buy and sell order processing, balance validations,
weighted-average price calculations, and transaction records.
"""

import uuid
from datetime import datetime
from typing import Optional
from fastapi import HTTPException
from sqlalchemy.orm import Session
try:
    from .models import Account, Holding, Order, Trade
    from .prices import get_server_price
except (ImportError, ValueError):
    from models import Account, Holding, Order, Trade
    from prices import get_server_price



def get_existing_idempotent_order(
    db: Session,
    user_id: str,
    idempotency_key: Optional[str],
) -> Optional[Order]:
    """Checks whether an order with the same idempotency key was already executed for this user."""
    if not idempotency_key or not idempotency_key.strip():
        return None
    return (
        db.query(Order)
        .filter(Order.user_id == user_id, Order.idempotency_key == idempotency_key.strip())
        .first()
    )


def execute_buy_order(
    db: Session,
    account: Account,
    symbol: str,
    quantity: int,
    idempotency_key: str = None,
) -> Order:
    """Executes a buy order atomically with balance and quantity validation."""
    clean_symbol = symbol.strip().upper()

    if quantity <= 0:
        raise HTTPException(
            status_code=400,
            detail=f"Order quantity must be greater than zero. Received: {quantity}",
        )

    # Resolve server-authoritative price (raises 400 if symbol invalid)
    price = get_server_price(clean_symbol)
    total_cost = round(price * quantity, 2)

    # Validate sufficient available funds
    if account.cash_balance < total_cost:
        raise HTTPException(
            status_code=400,
            detail=(
                f"Insufficient funds to buy {quantity} shares of {clean_symbol}. "
                f"Required: ₹{total_cost:,.2f}, Available: ₹{account.cash_balance:,.2f}"
            ),
        )

    try:
        # 1. Debit cash balance
        account.cash_balance = round(account.cash_balance - total_cost, 2)
        account.updated_at = datetime.utcnow()

        # 2. Update or insert holding with weighted-average buy price
        holding = (
            db.query(Holding)
            .filter(Holding.user_id == account.user_id, Holding.symbol == clean_symbol)
            .first()
        )

        if holding:
            old_qty = holding.quantity
            old_avg = holding.avg_buy_price
            new_qty = old_qty + quantity
            # Weighted average calculation: ((old_qty * old_avg) + total_cost) / new_qty
            new_avg = round(((old_qty * old_avg) + total_cost) / new_qty, 2)
            holding.quantity = new_qty
            holding.avg_buy_price = new_avg
            holding.updated_at = datetime.utcnow()
        else:
            holding = Holding(
                user_id=account.user_id,
                symbol=clean_symbol,
                quantity=quantity,
                avg_buy_price=round(price, 2),
                realized_pnl=0.0,
                updated_at=datetime.utcnow(),
            )
            db.add(holding)

        # 3. Create persistent Order entity
        order_id = f"ord_{uuid.uuid4().hex[:16]}"
        order = Order(
            id=order_id,
            user_id=account.user_id,
            symbol=clean_symbol,
            side="BUY",
            quantity=quantity,
            price=round(price, 2),
            total_value=total_cost,
            status="COMPLETED",
            idempotency_key=idempotency_key.strip() if idempotency_key else None,
            created_at=datetime.utcnow(),
        )
        db.add(order)

        # 4. Create persistent Trade transaction entity
        trade = Trade(
            order_id=order_id,
            user_id=account.user_id,
            symbol=clean_symbol,
            side="BUY",
            quantity=quantity,
            price=round(price, 2),
            total_value=total_cost,
            executed_at=datetime.utcnow(),
        )
        db.add(trade)

        db.commit()
        db.refresh(order)
        return order
    except HTTPException:
        db.rollback()
        raise
    except Exception as exc:
        db.rollback()
        raise HTTPException(
            status_code=500,
            detail=f"Failed to execute buy order: {str(exc)}",
        )


def execute_sell_order(
    db: Session,
    account: Account,
    symbol: str,
    quantity: int,
    idempotency_key: str = None,
) -> Order:
    """Executes a sell order atomically with share ownership validation and realized P&L."""
    clean_symbol = symbol.strip().upper()

    if quantity <= 0:
        raise HTTPException(
            status_code=400,
            detail=f"Order quantity must be greater than zero. Received: {quantity}",
        )

    # Resolve server-authoritative price
    price = get_server_price(clean_symbol)
    proceeds = round(price * quantity, 2)

    # Validate share ownership
    holding = (
        db.query(Holding)
        .filter(Holding.user_id == account.user_id, Holding.symbol == clean_symbol)
        .first()
    )

    owned = holding.quantity if holding else 0
    if owned < quantity:
        raise HTTPException(
            status_code=400,
            detail=(
                f"Insufficient shares to sell {quantity} of {clean_symbol}. "
                f"Owned: {owned}, Requested: {quantity}"
            ),
        )

    try:
        # Calculate realized P&L on the shares being sold
        # Realized P&L = (Sell Price - Avg Purchase Price) * Quantity
        avg_buy_price = holding.avg_buy_price
        sale_realized_pnl = round((price - avg_buy_price) * quantity, 2)

        # 1. Credit proceeds to cash balance and accumulate account realized P&L
        account.cash_balance = round(account.cash_balance + proceeds, 2)
        account.realized_pnl = round(account.realized_pnl + sale_realized_pnl, 2)
        account.updated_at = datetime.utcnow()

        # 2. Update holding quantity and holding-level realized P&L
        remaining_qty = holding.quantity - quantity
        holding.realized_pnl = round(holding.realized_pnl + sale_realized_pnl, 2)
        holding.updated_at = datetime.utcnow()

        if remaining_qty == 0:
            # When quantity reaches zero, delete holding entity so it does not appear in active holdings
            db.delete(holding)
        else:
            holding.quantity = remaining_qty

        # 3. Create persistent Order entity
        order_id = f"ord_{uuid.uuid4().hex[:16]}"
        order = Order(
            id=order_id,
            user_id=account.user_id,
            symbol=clean_symbol,
            side="SELL",
            quantity=quantity,
            price=round(price, 2),
            total_value=proceeds,
            status="COMPLETED",
            idempotency_key=idempotency_key.strip() if idempotency_key else None,
            created_at=datetime.utcnow(),
        )
        db.add(order)

        # 4. Create persistent Trade transaction entity
        trade = Trade(
            order_id=order_id,
            user_id=account.user_id,
            symbol=clean_symbol,
            side="SELL",
            quantity=quantity,
            price=round(price, 2),
            total_value=proceeds,
            executed_at=datetime.utcnow(),
        )
        db.add(trade)

        db.commit()
        db.refresh(order)
        return order
    except HTTPException:
        db.rollback()
        raise
    except Exception as exc:
        db.rollback()
        raise HTTPException(
            status_code=500,
            detail=f"Failed to execute sell order: {str(exc)}",
        )
