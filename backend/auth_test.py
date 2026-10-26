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

    if not token:
        raise RuntimeError("No access token returned")

    # Do not print or save the token in logs.
    client = GrowwAPI(token)

    print("Groww authentication succeeded.")
    print("Client initialised successfully.")

except Exception as exc:
    print("Groww authentication failed.")
    print("Error type:", type(exc).__name__)
    print("Check that the API key is approved and your subscription is active.")