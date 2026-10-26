import os
from dotenv import load_dotenv
from growwapi import GrowwAPI

load_dotenv()

api_key = os.getenv("GROWW_API_KEY")
secret = os.getenv("GROWW_API_SECRET")

if not api_key or not secret:
    raise SystemExit("Missing credentials in .env")

try:
    token = GrowwAPI.get_access_token(
        api_key=api_key,
        secret=secret,
    )
    groww = GrowwAPI(token)

    result = groww.get_ltp(
        segment=groww.SEGMENT_CASH,
        exchange_trading_symbols=("NSE_RELIANCE",),
    )

    print("Quote request succeeded.")
    print("Response:", result)

except Exception as exc:
    # Print SDK error details, never credentials or tokens.
    print("Error type:", type(exc).__name__)
    print("Message:", getattr(exc, "msg", ""))
    print("Error code:", getattr(exc, "code", ""))