"""Market data service module for TradeIQX.

Provides access to live stock quotes and market data via the Groww API SDK.
"""

import logging
import os
import re
import time
from datetime import datetime, timedelta, timezone
from functools import lru_cache
from typing import Any, Dict, List, Tuple

from dotenv import load_dotenv
from growwapi import GrowwAPI
from growwapi.groww.exceptions import (
    GrowwAPIAuthenticationException,
    GrowwAPIAuthorisationException,
    GrowwAPIException,
    GrowwAPINotFoundException,
    GrowwAPIRateLimitException,
    GrowwAPITimeoutException,
)

# Load .env relative to this file or current working directory
_dotenv_path = os.path.join(os.path.dirname(__file__), ".env")
if os.path.exists(_dotenv_path):
    load_dotenv(_dotenv_path)
else:
    load_dotenv()

logger = logging.getLogger("market_data")

DEFAULT_EXCHANGE_PREFIX: str = "NSE_"
DEFAULT_CURRENCY: str = "INR"
DEFAULT_SOURCE: str = "Groww"
DEFAULT_FRESHNESS: str = "not_verified"
GROWW_API_TIMEOUT_SECONDS: int = 10
STALE_QUOTE_THRESHOLD_SECONDS: int = 60

SYMBOL_REGEX = re.compile(r"^[A-Z0-9&-]{1,20}$")
SUPPORTED_HISTORY_RANGES: Tuple[str, ...] = ("1D", "1W", "1M", "1Y")


class MarketProviderAuthorisationError(Exception):
    """Raised when upstream market data provider returns 401/403 unauthorized."""
    pass


class MarketProviderRateLimitError(Exception):
    """Raised when upstream market data provider rate limits requests (429)."""
    pass


class MarketProviderUnavailableError(Exception):
    """Raised when market data provider credentials are missing, times out, or is offline."""
    pass


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


def validate_history_range(raw_range: str) -> str:
    """Validates and normalizes the requested historical chart time range.

    Args:
        raw_range: Time interval identifier (e.g. '1D', '1w', '1M', '1Y').

    Returns:
        str: Validated uppercase range string.

    Raises:
        ValueError: If raw_range is not one of ('1D', '1W', '1M', '1Y').
    """
    if not raw_range or not isinstance(raw_range, str):
        allowed = ", ".join(SUPPORTED_HISTORY_RANGES)
        raise ValueError(f"History range is required. Supported ranges: {allowed}")

    normalized = raw_range.strip().upper()
    if normalized not in SUPPORTED_HISTORY_RANGES:
        allowed = ", ".join(SUPPORTED_HISTORY_RANGES)
        raise ValueError(
            f"Unsupported history range '{raw_range}'. Supported ranges: {allowed}"
        )

    return normalized


@lru_cache(maxsize=1)
def get_groww_client() -> GrowwAPI:
    """Initializes and returns an authenticated singleton GrowwAPI client.

    Retrieves credentials from environment variables GROWW_API_KEY and
    GROWW_API_SECRET, exchanges them for an access token, and caches the client.

    Returns:
        GrowwAPI: Authenticated API client instance.

    Raises:
        MarketProviderUnavailableError: If Groww API credentials are not set.
        MarketProviderAuthorisationError: If credentials fail authentication with 401/403.
    """
    api_key = os.getenv("GROWW_API_KEY")
    secret = os.getenv("GROWW_API_SECRET")

    if not api_key or not secret:
        logger.warning("Groww API credentials missing in environment")
        raise MarketProviderUnavailableError("unavailable")

    try:
        logger.info("Authenticating with Groww API provider")
        token = GrowwAPI.get_access_token(
            api_key=api_key,
            secret=secret,
        )
        logger.info("Groww API authentication token exchanged successfully")
        return GrowwAPI(token)
    except (GrowwAPIAuthorisationException, GrowwAPIAuthenticationException):
        logger.error("Authentication with Groww API provider unauthorized (401/403)")
        raise MarketProviderAuthorisationError("provider_unauthorised") from None
    except (GrowwAPITimeoutException, GrowwAPIException, Exception) as exc:
        logger.error("Authentication with Groww API provider failed: %s", type(exc).__name__)
        raise MarketProviderUnavailableError("unavailable") from None


def get_live_quote(symbol: str) -> Dict[str, Any]:
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
            - previous_close (float or None): Previous day closing price.
            - change (float or None): Absolute price change.
            - change_percent (float or None): Percentage change.
            - status (str): Market data status ("LIVE", "DELAYED", "UNAUTHORISED", "OFFLINE").

    Raises:
        ValueError: If symbol is empty or invalid.
        LookupError: If no valid price quote is returned by the upstream provider.
        MarketProviderAuthorisationError: If upstream client is unauthorized (403).
        MarketProviderRateLimitError: If provider rate limit exceeded (429).
        MarketProviderUnavailableError: If provider is offline or times out.
    """
    symbol = normalize_symbol(symbol)
    trading_symbol = f"{DEFAULT_EXCHANGE_PREFIX}{symbol}"
    logger.info("Fetching quote for symbol=%s", symbol)

    try:
        client = get_groww_client()
        result = client.get_ltp(
            segment=client.SEGMENT_CASH,
            exchange_trading_symbols=(trading_symbol,),
        )
    except (GrowwAPIAuthorisationException, GrowwAPIAuthenticationException):
        raise MarketProviderAuthorisationError("provider_unauthorised") from None
    except GrowwAPIRateLimitException:
        raise MarketProviderRateLimitError("rate_limited") from None
    except GrowwAPINotFoundException:
        raise LookupError(f"No quote returned for symbol {symbol}") from None
    except (MarketProviderAuthorisationError, MarketProviderRateLimitError, MarketProviderUnavailableError):
        raise
    except Exception as exc:
        logger.warning("Upstream market provider call failed for symbol=%s: %s", symbol, type(exc).__name__)
        raise MarketProviderUnavailableError("unavailable") from None

    price = result.get(trading_symbol)

    if price is None or float(price) <= 0:
        logger.warning("No valid price quote found for symbol=%s", symbol)
        raise LookupError("No valid quote returned for this symbol")

    logger.info("Quote fetched successfully for symbol=%s price=%.2f", symbol, float(price))

    return {
        "symbol": symbol,
        "price": float(price),
        "currency": DEFAULT_CURRENCY,
        "source": DEFAULT_SOURCE,
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "freshness": "live",
        "previous_close": None,
        "change": None,
        "change_percent": None,
        "status": "LIVE",
    }


def get_stock_history(symbol: str, range_str: str = "1D") -> Dict[str, Any]:
    """Fetches real historical price candles for the specified symbol and range via Groww SDK.

    Maps ranges 1D/1W/1M/1Y to supported intervals, executes live SDK historical candle call,
    and validates ascending timestamps and OHLC validity. Never fabricates synthetic candles.

    Args:
        symbol: Stock ticker symbol.
        range_str: History time range ('1D', '1W', '1M', '1Y').

    Returns:
        dict: Object containing symbol, range, validated candles list, and status.

    Raises:
        ValueError: If symbol or range is invalid.
        LookupError: If symbol is not found.
        MarketProviderAuthorisationError: If Groww credentials lack permissions (403).
        MarketProviderRateLimitError: If rate limited (429).
        MarketProviderUnavailableError: If provider is offline or times out (503).
    """
    clean_symbol = normalize_symbol(symbol)
    clean_range = validate_history_range(range_str)
    logger.info("Fetching real history for symbol=%s range=%s", clean_symbol, clean_range)

    client = get_groww_client()
    trading_symbol = f"{DEFAULT_EXCHANGE_PREFIX}{clean_symbol}"

    now = datetime.now(timezone.utc)
    if clean_range == "1D":
        start_time = now - timedelta(days=1)
        interval_min = 5
    elif clean_range == "1W":
        start_time = now - timedelta(days=7)
        interval_min = 60
    elif clean_range == "1M":
        start_time = now - timedelta(days=30)
        interval_min = 1440
    elif clean_range == "1Y":
        start_time = now - timedelta(days=365)
        interval_min = 1440
    else:
        raise ValueError(f"Unsupported history range: {clean_range}")

    start_str = start_time.strftime("%Y-%m-%d %H:%M:%S")
    end_str = now.strftime("%Y-%m-%d %H:%M:%S")

    try:
        raw_response = client.get_historical_candle_data(
            trading_symbol=trading_symbol,
            exchange="NSE",
            segment=client.SEGMENT_CASH,
            start_time=start_str,
            end_time=end_str,
            interval_in_minutes=interval_min,
            timeout=GROWW_API_TIMEOUT_SECONDS,
        )
    except (GrowwAPIAuthorisationException, GrowwAPIAuthenticationException):
        raise MarketProviderAuthorisationError("provider_unauthorised") from None
    except GrowwAPIRateLimitException:
        raise MarketProviderRateLimitError("rate_limited") from None
    except GrowwAPINotFoundException:
        raise LookupError(f"No history found for symbol {clean_symbol}") from None
    except (MarketProviderAuthorisationError, MarketProviderRateLimitError, MarketProviderUnavailableError):
        raise
    except Exception as exc:
        logger.warning("Upstream history call failed: %s", type(exc).__name__)
        raise MarketProviderUnavailableError("unavailable") from None

    # Parse and validate candles strictly (ascending timestamps, high>=max(open,close), low<=min)
    raw_candles = raw_response.get("candles") if isinstance(raw_response, dict) else []
    validated_candles: List[Dict[str, Any]] = []
    last_timestamp = -1

    if raw_candles:
        for item in raw_candles:
            if isinstance(item, (list, tuple)) and len(item) >= 5:
                ts, o, h, l, c = item[0], float(item[1]), float(item[2]), float(item[3]), float(item[4])
                v = int(item[5]) if len(item) > 5 else 0
            elif isinstance(item, dict):
                ts = item.get("timestamp") or item.get("time")
                o, h, l, c = float(item["open"]), float(item["high"]), float(item["low"]), float(item["close"])
                v = int(item.get("volume", 0))
            else:
                continue

            # Ascending timestamp validation
            if ts is None or (last_timestamp != -1 and ts < last_timestamp):
                continue

            # OHLC integrity check
            if h < max(o, c) or l > min(o, c) or l < 0:
                continue

            validated_candles.append({
                "timestamp": int(ts),
                "open": round(o, 2),
                "high": round(h, 2),
                "low": round(l, 2),
                "close": round(c, 2),
                "volume": v,
            })
            last_timestamp = ts

    return {
        "symbol": clean_symbol,
        "range": clean_range,
        "candles": validated_candles,
        "status": "LIVE",
    }
