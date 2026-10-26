import os
import sys
from pathlib import Path
import unittest

THROWAWAY_DB = Path(__file__).resolve().parent / "test_throwaway.db"
if THROWAWAY_DB.exists():
    try:
        THROWAWAY_DB.unlink()
    except Exception:
        pass

os.environ["DATABASE_URL"] = f"sqlite:///{THROWAWAY_DB}"
os.environ["OLLAMA_URL"] = "http://127.0.0.1:54321"

from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


class TestLocalDefaultsE2E(unittest.TestCase):

    @classmethod
    def tearDownClass(cls):
        if THROWAWAY_DB.exists():
            try:
                THROWAWAY_DB.unlink()
            except Exception:
                pass

    def test_01_health_check(self):
        resp = client.get("/health")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json(), {"status": "ok"})

    def test_02_ready_check(self):
        resp = client.get("/ready")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()["status"], "ready")

    def test_03_auth_flow_and_profile(self):
        # 1. Signup
        signup_payload = {
            "username": "deploytester",
            "email": "deploytester@gmail.com",
            "password": "Password123"
        }
        res_signup = client.post("/auth/signup", json=signup_payload)
        self.assertEqual(res_signup.status_code, 201)
        user_id = res_signup.json()["id"]

        # 2. Login
        login_payload = {
            "email": "deploytester@gmail.com",
            "password": "Password123"
        }
        res_login = client.post("/auth/login", json=login_payload)
        self.assertEqual(res_login.status_code, 200)
        self.assertIn("user_id", res_login.json())

        # 3. /auth/me
        res_me = client.get(f"/auth/me?user_id={user_id}")
        self.assertEqual(res_me.status_code, 200)
        self.assertEqual(res_me.json()["email"], "deploytester@gmail.com")

        # 4. /portfolio
        res_port = client.get("/portfolio", headers={"X-User-Id": str(user_id)})
        self.assertEqual(res_port.status_code, 200)
        self.assertIn("cash_balance", res_port.json())

        # 5. /buy
        buy_payload = {
            "symbol": "RELIANCE",
            "quantity": 2,
            "price": 2500.0
        }
        res_buy = client.post("/buy", json=buy_payload, headers={"X-User-Id": str(user_id)})
        self.assertEqual(res_buy.status_code, 200)
        self.assertIn("portfolio", res_buy.json())

        # 6. /sell
        sell_payload = {
            "symbol": "RELIANCE",
            "quantity": 1,
            "price": 2550.0
        }
        res_sell = client.post("/sell", json=sell_payload, headers={"X-User-Id": str(user_id)})
        self.assertEqual(res_sell.status_code, 200)
        self.assertIn("portfolio", res_sell.json())

    def test_04_market_quote_reliance(self):
        res = client.get("/market/quote/RELIANCE")
        # 200 with quote, or 403 provider_unauthorised, or 503 unavailable
        self.assertIn(res.status_code, [200, 403, 503])
        print(f"\n[Market Quote Status for RELIANCE]: HTTP {res.status_code}")

    def test_05_ai_chat_graceful_offline(self):
        res = client.post("/api/chat", json={"message": "What is RSI?"})
        # Graceful failure when Ollama is offline (503 Service Unavailable)
        self.assertIn(res.status_code, [503, 500])

    def test_06_ai_quiz_graceful_offline(self):
        res = client.post(
            "/api/quiz/generate",
            json={"topic": "Stock Market Basics", "difficulty": "Beginner"}
        )
        # Graceful failure when Ollama is offline (503 Service Unavailable)
        self.assertIn(res.status_code, [503, 500])


if __name__ == "__main__":
    unittest.main()
