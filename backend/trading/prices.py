"""Server-authoritative stock price table for TradeIQX Trading Engine.

The server strictly determines execution prices and never trusts client-sent prices.
Prices and symbols are aligned with MarketWatchScreen demo assets.
"""

from typing import Dict, Any, Optional
from fastapi import HTTPException

# Authoritative stock prices and company metadata
SERVER_STOCK_CATALOG: Dict[str, Dict[str, Any]] = {
    "RELIANCE": {
        "name": "Reliance Industries",
        "price": 1450.25,
    },
    "TCS": {
        "name": "Tata Consultancy Services",
        "price": 3920.50,
    },
    "INFY": {
        "name": "Infosys Limited",
        "price": 1620.30,
    },
    "HDFCBANK": {
        "name": "HDFC Bank",
        "price": 1780.90,
    },
    "ICICIBANK": {
        "name": "ICICI Bank",
        "price": 1245.60,
    },
    "ITC": {
        "name": "ITC Limited",
        "price": 420.80,
    },
    "SBIN": {
        "name": "State Bank of India",
        "price": 725.60,
    },
    "WIPRO": {
        "name": "Wipro Limited",
        "price": 290.40,
    },
}


def get_stock_info(symbol: str) -> Dict[str, Any]:
    """Retrieves authoritative stock details or raises a 400 Bad Request error."""
    clean_symbol = symbol.strip().upper()
    if clean_symbol not in SERVER_STOCK_CATALOG:
        available = ", ".join(sorted(SERVER_STOCK_CATALOG.keys()))
        raise HTTPException(
            status_code=400,
            detail=f"Unknown stock symbol '{symbol}'. Available symbols: {available}",
        )
    return {
        "symbol": clean_symbol,
        "name": SERVER_STOCK_CATALOG[clean_symbol]["name"],
        "price": SERVER_STOCK_CATALOG[clean_symbol]["price"],
    }


def get_server_price(symbol: str) -> float:
    """Returns the authoritative execution price for a given ticker symbol."""
    return get_stock_info(symbol)["price"]
