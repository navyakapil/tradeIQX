"""TradeIQX Backend End-to-End Smoke Test.

Verifies:
1. Manual trading calculations:
   - Buy 10 @ 100 then 10 @ 120 -> avg 110
   - Sell 5 @ 130 -> realized P&L = 100
   - Oversell rejected (HTTP 400)
   - Insufficient cash rejected (HTTP 400)
   - Empty portfolio returns safe zero metrics
   - Data persistence across DB close/reopen (app restart simulation)
2. RAG Chatbot Retrieval:
   - 5 questions: what is a stock, market vs limit order, what is a stop-loss, why diversify, how much to risk per trade
   - Verifies relevant topics retrieved and answer preview generated
"""

import os
import sys
import tempfile
from pathlib import Path

# Setup paths
BACKEND_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = BACKEND_DIR.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

# Use isolated temp database for smoke test
temp_db_fd, temp_db_path = tempfile.mkstemp(suffix="_smoke_tradeiqx.db")
os.close(temp_db_fd)
os.environ["DATABASE_URL"] = f"sqlite:///{temp_db_path}"

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from fastapi.testclient import TestClient

from database import Base
import models
from main import app, get_db

# Create test engine and session
test_engine = create_engine(
    f"sqlite:///{temp_db_path}",
    connect_args={"check_same_thread": False}
)
TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)
Base.metadata.create_all(bind=test_engine)


def override_get_db():
    db = TestSessionLocal()
    try:
        yield db
    finally:
        db.close()


app.dependency_overrides[get_db] = override_get_db
client = TestClient(app)


def run_trading_smoke_tests():
    print("=" * 60)
    print("RUNNING TRADING ENGINE & PORTFOLIO PERSISTENCE TESTS")
    print("=" * 60)

    test_user_id = 999
    headers = {"X-User-Id": str(test_user_id)}

    # Test 1: Empty Portfolio Returns Safe Zeros
    resp = client.get("/api/portfolio", headers=headers)
    assert resp.status_code == 200, f"Failed empty portfolio: {resp.text}"
    port = resp.json()
    assert port["cash"] == 1_000_000.0, f"Expected 1M cash, got {port['cash']}"
    assert port["total_invested"] == 0.0
    assert port["unrealized_pnl"] == 0.0
    assert port["return_pct"] == 0.0
    assert len(port["holdings_detail"]) == 0
    print("[PASS] Test 1 Passed: Empty portfolio returns zero-safe metrics and 10L cash.")

    # Test 2: Buy 10 @ 100
    buy1 = client.post(
        "/api/trade/buy",
        json={"symbol": "RELIANCE", "quantity": 10, "price": 100.0},
        headers=headers
    )
    assert buy1.status_code == 200, f"Buy 1 failed: {buy1.text}"
    assert buy1.json()["transaction"]["average_buy_price"] == 100.0
    print("[PASS] Test 2 Passed: Buy 10 @ 100 recorded. Avg price = 100.0")

    # Test 3: Buy 10 @ 120 -> Weighted Avg = 110
    buy2 = client.post(
        "/api/trade/buy",
        json={"symbol": "RELIANCE", "quantity": 10, "price": 120.0},
        headers=headers
    )
    assert buy2.status_code == 200, f"Buy 2 failed: {buy2.text}"
    avg = buy2.json()["transaction"]["average_buy_price"]
    assert avg == 110.0, f"Expected avg price 110.0, got {avg}"
    print("[PASS] Test 3 Passed: Buy 10 @ 120 merged into holding. Weighted Avg = 110.0")

    # Test 4: Sell 5 @ 130 -> Realized P&L = 5 * (130 - 110) = 100
    sell1 = client.post(
        "/api/trade/sell",
        json={"symbol": "RELIANCE", "quantity": 5, "price": 130.0},
        headers=headers
    )
    assert sell1.status_code == 200, f"Sell 1 failed: {sell1.text}"
    txn = sell1.json()["transaction"]
    realized = txn["realized_pnl"]
    remaining_qty = txn["remaining_quantity"]
    assert realized == 100.0, f"Expected realized P&L 100.0, got {realized}"
    assert remaining_qty == 15, f"Expected 15 remaining shares, got {remaining_qty}"
    print("[PASS] Test 4 Passed: Sell 5 @ 130 recorded. Realized P&L = +100.0, Remaining = 15")

    # Test 5: Oversell Rejected
    oversell = client.post(
        "/api/trade/sell",
        json={"symbol": "RELIANCE", "quantity": 50, "price": 130.0},
        headers=headers
    )
    assert oversell.status_code == 400, f"Expected 400, got {oversell.status_code}"
    assert "Insufficient shares" in oversell.json()["detail"]
    print("[PASS] Test 5 Passed: Oversell attempt rejected with HTTP 400 Insufficient shares.")

    # Test 6: Insufficient Cash Rejected
    huge_buy = client.post(
        "/api/trade/buy",
        json={"symbol": "RELIANCE", "quantity": 1_000_000, "price": 1450.0},
        headers=headers
    )
    assert huge_buy.status_code == 400, f"Expected 400, got {huge_buy.status_code}"
    assert "Insufficient virtual cash" in huge_buy.json()["detail"]
    print("[PASS] Test 6 Passed: Insufficient cash buy rejected with HTTP 400 Insufficient virtual cash.")

    # Test 7: Persistence Simulation (Re-opening DB connection)
    test_engine.dispose()
    new_engine = create_engine(f"sqlite:///{temp_db_path}")
    NewSession = sessionmaker(bind=new_engine)
    with NewSession() as session:
        holding = session.query(models.Holding).filter(
            models.Holding.user_id == test_user_id,
            models.Holding.symbol == "RELIANCE"
        ).first()
        assert holding is not None, "Holding did not survive restart simulation"
        assert holding.quantity == 15, f"Expected 15 shares, got {holding.quantity}"
        assert holding.average_price == 110.0, f"Expected avg 110.0, got {holding.average_price}"

        trades = session.query(models.Trade).filter(models.Trade.user_id == test_user_id).all()
        assert len(trades) == 3, f"Expected 3 trades, got {len(trades)}"
    new_engine.dispose()
    print("[PASS] Test 7 Passed: Data persistence verified across database reconnect.")


def run_rag_mentor_tests():
    print("\n" + "=" * 60)
    print("RUNNING RAG CHATBOT RETRIEVAL & ANSWER TESTS")
    print("=" * 60)

    from rag.tradeiqx_retriever import smart_search, build_rag_context_with_scores
    from rag_service import answer_question

    test_queries = [
        "what is a stock",
        "market vs limit order",
        "what is a stop-loss",
        "why diversify",
        "how much to risk per trade"
    ]

    for q in test_queries:
        res = build_rag_context_with_scores(q)
        confidence = res.get("confidence", 0.0)
        sources = res.get("sources", [])
        assert confidence > 0.3, f"Low confidence for query '{q}': {confidence}"
        assert len(sources) > 0, f"No sources for query '{q}'"

        ans_obj = answer_question(q)
        answer_preview = ans_obj["answer"].split("\n")[0][:100]

        print(f"Query: '{q}'")
        print(f"  Confidence: {confidence:.3f}")
        print(f"  Retrieved Topics: {sources}")
        print(f"  Answer Preview: {answer_preview}...")
        print("-" * 50)

    print("[PASS] All 5 RAG Trading Mentor queries retrieved and verified.")


if __name__ == "__main__":
    try:
        run_trading_smoke_tests()
        run_rag_mentor_tests()
        print("\n" + "*" * 60)
        print("ALL SMOKE TESTS COMPLETED SUCCESSFULLY!")
        print("*" * 60)
    finally:
        # Cleanup temp DB
        try:
            if os.path.exists(temp_db_path):
                os.remove(temp_db_path)
        except Exception:
            pass
