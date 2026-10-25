
import os
from datetime import datetime, timezone
from functools import lru_cache

from dotenv import load_dotenv
from growwapi import GrowwAPI

load_dotenv()


@lru_cache(maxsize=1)
def get_groww_client():
    api_key = os.getenv("GROWW_API_KEY")
    secret = os.getenv("GROWW_API_SECRET")

    if not api_key or not secret:
        raise RuntimeError("Groww API credentials are not configured")

    token = GrowwAPI.get_access_token(
        api_key=api_key,
        secret=secret,
    )
    return GrowwAPI(token)


def get_live_quote(symbol: str) -> dict:
    symbol = symbol.strip().upper()

    if not symbol.isalnum() or not symbol:
        raise ValueError("Invalid stock symbol")

    client = get_groww_client()
    trading_symbol = f"NSE_{symbol}"

    result = client.get_ltp(
        segment=client.SEGMENT_CASH,
        exchange_trading_symbols=(trading_symbol,),
    )

    price = result.get(trading_symbol)

    if price is None or float(price) <= 0:
        raise LookupError("No valid quote returned for this symbol")

    return {
        "symbol": symbol,
        "price": float(price),
        "source": "Groww",
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "freshness": "not_verified",
    }
