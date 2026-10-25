import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from database import Base
from models import User, Portfolio, Holding


class TestPortfolioPersistence(unittest.TestCase):

    def test_portfolio_survives_reconnection(self):
        with TemporaryDirectory() as folder:
            db_path = Path(folder) / "portfolio_test.db"
            db_url = f"sqlite:///{db_path.as_posix()}"

            engine = create_engine(db_url)
            Base.metadata.create_all(bind=engine)

            Session = sessionmaker(bind=engine)

            with Session() as db:
                user = User(
                    username="test_persistence_user",
                    email="persistence@example.com",
                    hashed_password="test_hash"
                )
                db.add(user)
                db.flush()

                portfolio = Portfolio(
                    user_id=user.id,
                    cash_balance=97000.0
                )
                holding = Holding(
                    user_id=user.id,
                    symbol="RELIANCE",
                    quantity=2,
                    average_price=1500.0
                )

                db.add_all([portfolio, holding])
                db.commit()

            engine.dispose()

            new_engine = create_engine(db_url)
            NewSession = sessionmaker(bind=new_engine)

            try:
                with NewSession() as db:
                    saved_portfolio = db.query(Portfolio).first()
                    saved_holding = db.query(Holding).first()

                    self.assertIsNotNone(saved_portfolio)
                    self.assertIsNotNone(saved_holding)

                    self.assertEqual(
                        saved_portfolio.cash_balance, 97000.0
                    )
                    self.assertEqual(saved_holding.quantity, 2)
                    self.assertEqual(
                        saved_holding.average_price, 1500.0
                    )
            finally:
                new_engine.dispose()


if __name__ == "__main__":
    unittest.main()