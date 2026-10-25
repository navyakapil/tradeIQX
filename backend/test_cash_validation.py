import unittest
from test_db_setup import isolated_test_client
from models import User, Portfolio


class TestCashValidation(unittest.TestCase):

    def test_insufficient_cash(self):
        with isolated_test_client() as (client, TestSession):

            with TestSession() as db:
                user = User(
                    username="cash_test_user",
                    email="cash_test@example.com",
                    hashed_password="test_hash"
                )
                db.add(user)
                db.flush()

                portfolio = Portfolio(
                    user_id=user.id,
                    cash_balance=10000.0
                )
                db.add(portfolio)
                db.commit()

            response = client.post(
                "/buy",
                json={
                    "symbol": "RELIANCE",
                    "quantity": 100,
                    "price": 1500.0
                }
            )

            self.assertEqual(response.status_code, 400)
            self.assertEqual(
                response.json()["detail"],
                "Insufficient virtual cash"
            )


if __name__ == "__main__":
    unittest.main()