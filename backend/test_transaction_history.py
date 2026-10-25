import unittest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


class TestTransactionHistory(unittest.TestCase):

    def test_transaction_history_format(self):
        response = client.get("/portfolio")

        self.assertEqual(response.status_code, 200)

        data = response.json()

        self.assertIn("transactions", data)
        self.assertIsInstance(data["transactions"], list)

        for trade in data["transactions"]:
            self.assertIn("symbol", trade)
            self.assertIn("side", trade)
            self.assertIn("quantity", trade)
            self.assertIn("price", trade)
            self.assertIn("total", trade)
            self.assertIn("created_at", trade)

            self.assertIn(trade["side"], ["BUY", "SELL"])
            self.assertEqual(
                trade["total"],
                round(trade["quantity"] * trade["price"], 2)
            )


if __name__ == "__main__":
    unittest.main()