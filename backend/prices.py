"""Static price table for TradeIQX paper trading.

Matches the stocks and default quotes displayed in MarketWatchScreen.kt.
"""

from typing import Dict

STATIC_PRICES: Dict[str, float] = {
    "RELIANCE": 1450.25,
    "TCS": 3920.50,
    "INFY": 1620.30,
    "HDFCBANK": 1780.90,
    "ICICIBANK": 1245.60,
    "ITC": 420.80,
    "SBIN": 725.60,
    "WIPRO": 290.40,
    "TATAMOTORS": 980.50,
    "BHARTIARTL": 1560.00,
    "KOTAKBANK": 1750.00,
    "LT": 3600.00,
}


def get_current_price(symbol: str) -> float:
    """Returns the current market price for a stock symbol."""
    sym = symbol.strip().upper()
    return STATIC_PRICES.get(sym, 1000.0)
