"""Execution services for TradeIQX Trading Engine.

Handles atomic buy and sell order processing, balance validations,
weighted-average price calculations, and transaction records.
"""

import uuid
from datetime import datetime
from fastapi import HTTPException
from sqlalchemy.orm import Session
from models import Account, Holding, Order, Trade
from prices import get_server_price


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
            idempotency_key=idempotency_key,
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
