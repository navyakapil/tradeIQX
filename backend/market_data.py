"""Market data service module for TradeIQX.

Provides access to live stock quotes and market data via the Groww API SDK.
"""

import os
import re
from datetime import datetime, timezone
from functools import lru_cache

from dotenv import load_dotenv
from growwapi import GrowwAPI

load_dotenv()

DEFAULT_EXCHANGE_PREFIX = "NSE_"
DEFAULT_CURRENCY = "INR"
DEFAULT_SOURCE = "Groww"
DEFAULT_FRESHNESS = "not_verified"
GROWW_API_TIMEOUT_SECONDS = 10
STALE_QUOTE_THRESHOLD_SECONDS = 60

SYMBOL_REGEX = re.compile(r"^[A-Z0-9&-]{1,20}$")


def normalize_symbol(raw_symbol: str) -> str:
    """Normalizes and validates a stock ticker symbol.

    Strips whitespace, converts to uppercase, removes optional 'NSE:' / 'NSE_'
    prefixes and '.NS' suffixes, and validates against allowed format.

    Args:
        raw_symbol: Input ticker string (e.g. 'reliance.ns', 'NSE_TCS', ' infy ').

    Returns:
        str: Cleaned uppercase symbol (e.g. 'RELIANCE', 'TCS', 'INFY').

    Raises:
        ValueError: If symbol is empty or contains invalid characters.
    """
    if not raw_symbol or not isinstance(raw_symbol, str):
        raise ValueError("Stock symbol must be a non-empty string")

    symbol = raw_symbol.strip().upper()

    if symbol.startswith("NSE:") or symbol.startswith("NSE_"):
        symbol = symbol[4:].strip()

    if symbol.endswith(".NS"):
        symbol = symbol[:-3].strip()

    if not symbol or not SYMBOL_REGEX.match(symbol):
        raise ValueError(
            f"Invalid stock symbol '{raw_symbol}'. Must be 1-20 alphanumeric characters."
        )

    return symbol


@lru_cache(maxsize=1)
def get_groww_client() -> GrowwAPI:
    """Initializes and returns an authenticated singleton GrowwAPI client.

    Retrieves credentials from environment variables GROWW_API_KEY and
    GROWW_API_SECRET, exchanges them for an access token, and caches the client.

    Returns:
        GrowwAPI: Authenticated API client instance.

    Raises:
        RuntimeError: If Groww API credentials are not set or authentication fails.
    """
    api_key = os.getenv("GROWW_API_KEY")
    secret = os.getenv("GROWW_API_SECRET")

    if not api_key or not secret:
        raise RuntimeError("Market data provider credentials are not configured")

    try:
        token = GrowwAPI.get_access_token(
            api_key=api_key,
            secret=secret,
        )
        return GrowwAPI(token)
    except Exception:
        raise RuntimeError("Failed to authenticate with market data provider") from None


def get_live_quote(symbol: str) -> dict:
    """Fetches the latest traded price (LTP) for a given stock symbol.

    Args:
        symbol: The stock ticker symbol (e.g. 'RELIANCE', 'TCS').

    Returns:
        dict: A dictionary containing quote details including:
            - symbol (str): The normalized stock symbol.
            - price (float): Latest traded price in INR.
            - currency (str): Currency ISO code (INR).
            - source (str): Data provider name ("Groww").
            - fetched_at (str): ISO 8601 UTC timestamp of retrieval.
            - freshness (str): Freshness status of the quote.

    Raises:
        ValueError: If symbol is empty or contains non-alphanumeric characters.
        LookupError: If no valid price quote is returned by the upstream provider.
        RuntimeError: If upstream client authentication or connection fails.
    """
    symbol = normalize_symbol(symbol)
    trading_symbol = f"{DEFAULT_EXCHANGE_PREFIX}{symbol}"

    try:
        client = get_groww_client()
        result = client.get_ltp(
            segment=client.SEGMENT_CASH,
            exchange_trading_symbols=(trading_symbol,),
        )
    except (ValueError, LookupError, RuntimeError):
        raise
    except Exception:
        raise RuntimeError("Upstream market provider request failed") from None

    price = result.get(trading_symbol)

    if price is None or float(price) <= 0:
        raise LookupError("No valid quote returned for this symbol")

    return {
        "symbol": symbol,
        "price": float(price),
        "currency": DEFAULT_CURRENCY,
        "source": DEFAULT_SOURCE,
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "freshness": DEFAULT_FRESHNESS,
    }
