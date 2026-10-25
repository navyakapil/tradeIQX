import unittest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


class TestCashValidation(unittest.TestCase):

    def test_insufficient_cash(self):
        response = client.post(
            "/buy",
            json={
                "symbol": "RELIANCE",
                "quantity": 100000,
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