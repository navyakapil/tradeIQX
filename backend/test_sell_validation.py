import unittest

from test_db_setup import isolated_test_client
from models import User, Portfolio, Holding


class TestSellValidation(unittest.TestCase):

    def test_insufficient_shares(self):
        with isolated_test_client() as (client, TestSession):

            with TestSession() as db:
                user = User(
                    username="sell_test_user",
                    email="sell_test@example.com",
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

            response = client.post(
                "/sell",
                json={
                    "symbol": "RELIANCE",
                    "quantity": 100,
                    "price": 1500.0
                }
            )

            self.assertEqual(response.status_code, 400)
            self.assertEqual(
                response.json()["detail"],
                "Insufficient shares to sell"
            )


if __name__ == "__main__":
    unittest.main()