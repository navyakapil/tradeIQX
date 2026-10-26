"""TradeIQX Test Data Seeder.

Prepares an idempotent demo trader account with realistic sample holdings,
orders, and executed trades for on-device and local manual testing.
NOT run automatically on backend startup.
"""

import sys
from pathlib import Path

# Add backend directory to path
BACKEND_DIR = Path(__file__).resolve().parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

from database import SessionLocal, engine
from models import Base, User, Account, Portfolio, Holding, Order, Trade

DEMO_USER_ID = 1
DEMO_USERNAME = "demo_trader"
DEMO_EMAIL = "demo@tradeiqx.com"


def seed_demo_data(reset: bool = False):
    """Seeds the database with sample demo account and trades."""
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    try:
        # Check or create demo user
        user = db.query(User).filter(User.id == DEMO_USER_ID).first()
        if not user:
            user = User(
                id=DEMO_USER_ID,
                username=DEMO_USERNAME,
                email=DEMO_EMAIL,
                hashed_password="$bcrypt-sha256$v=2,t=2b,r=12$dummyhashfordemotesting"
            )
            db.add(user)
            db.commit()
            db.refresh(user)
            print(f"Created demo user: {user.username} (id={user.id})")

        # Check existing trades
        existing_trades = db.query(Trade).filter(Trade.user_id == DEMO_USER_ID).count()
        if existing_trades > 0 and not reset:
            print(f"Demo user {DEMO_USER_ID} already has {existing_trades} trades. Seeding skipped (idempotent).")
            account = db.query(Account).filter(Account.user_id == DEMO_USER_ID).first()
            cash = account.cash if account else 1_000_000.0
            print(f"Current cash balance: ₹{cash:,.2f}")
            return

        if reset:
            print("Reset flag provided. Clearing existing demo user trades and holdings...")
            db.query(Trade).filter(Trade.user_id == DEMO_USER_ID).delete()
            db.query(Order).filter(Order.user_id == DEMO_USER_ID).delete()
            db.query(Holding).filter(Holding.user_id == DEMO_USER_ID).delete()
            db.commit()

        # Seed initial Account and Portfolio
        account = db.query(Account).filter(Account.user_id == DEMO_USER_ID).first()
        portfolio = db.query(Portfolio).filter(Portfolio.user_id == DEMO_USER_ID).first()

        starting_cash = 1_000_000.0
        if not account:
            account = Account(user_id=DEMO_USER_ID, cash=starting_cash)
            db.add(account)
        else:
            account.cash = starting_cash

        if not portfolio:
            portfolio = Portfolio(user_id=DEMO_USER_ID, cash_balance=starting_cash)
            db.add(portfolio)
        else:
            portfolio.cash_balance = starting_cash

        db.commit()

        # Trades plan:
        # 1. BUY RELIANCE 10 @ 1400.00
        # 2. BUY RELIANCE 10 @ 1440.00 (avg = 1420.00, qty = 20)
        # 3. BUY TCS 10 @ 3800.00
        # 4. SELL TCS 4 @ 3950.00 (realized = +600.00, remaining = 6)
        # 5. BUY INFY 20 @ 1600.00

        trades_plan = [
            {"side": "BUY", "symbol": "RELIANCE", "qty": 10, "price": 1400.00, "realized": 0.0},
            {"side": "BUY", "symbol": "RELIANCE", "qty": 10, "price": 1440.00, "realized": 0.0},
            {"side": "BUY", "symbol": "TCS", "qty": 10, "price": 3800.00, "realized": 0.0},
            {"side": "SELL", "symbol": "TCS", "qty": 4, "price": 3950.00, "realized": 600.00},
            {"side": "BUY", "symbol": "INFY", "qty": 20, "price": 1600.00, "realized": 0.0},
        ]

        current_cash = starting_cash

        for t in trades_plan:
            total = round(t["qty"] * t["price"], 2)
            order = Order(
                user_id=DEMO_USER_ID,
                side=t["side"],
                symbol=t["symbol"],
                quantity=t["qty"],
                price=t["price"],
                status="EXECUTED"
            )
            db.add(order)
            db.flush()

            trade = Trade(
                user_id=DEMO_USER_ID,
                order_id=order.id,
                symbol=t["symbol"],
                side=t["side"],
                quantity=t["qty"],
                price=t["price"],
                total=total,
                realized_pnl=t["realized"]
            )
            db.add(trade)

            if t["side"] == "BUY":
                current_cash -= total
            else:
                current_cash += total

        # Setup holdings
        holdings_plan = [
            {"symbol": "RELIANCE", "qty": 20, "avg_price": 1420.00},
            {"symbol": "TCS", "qty": 6, "avg_price": 3800.00},
            {"symbol": "INFY", "qty": 20, "avg_price": 1600.00},
        ]

        for h in holdings_plan:
            holding = Holding(
                user_id=DEMO_USER_ID,
                symbol=h["symbol"],
                quantity=h["qty"],
                average_price=h["avg_price"]
            )
            db.add(holding)

        account.cash = round(current_cash, 2)
        portfolio.cash_balance = round(current_cash, 2)
        db.commit()

        print(f"Successfully seeded demo trader {DEMO_USER_ID}!")
        print(f"- Cash balance: ₹{current_cash:,.2f}")
        print(f"- Orders and trades created: {len(trades_plan)}")
        print(f"- Holdings created: {len(holdings_plan)}")

    finally:
        db.close()


if __name__ == "__main__":
    reset_arg = "--reset" in sys.argv
    seed_demo_data(reset=reset_arg)
