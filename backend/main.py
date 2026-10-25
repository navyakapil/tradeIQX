
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="TradeIQX API")


@app.get("/")
def home():
    return {"message": "TradeIQX backend is running!"}


@app.get("/health")
def health_check():
    return {"status": "healthy"}


class StockAnalysisRequest(BaseModel):
    symbol: str
    signal: str
    risk: str
    indicators: str


@app.post("/analyze")
def analyze_stock(data: StockAnalysisRequest):
    explanation = (
        f"{data.symbol} has a simulated {data.signal} signal "
        f"with a {data.risk} risk indicator. "
        f"Technical analysis: {data.indicators} "
        "This explanation is for educational paper trading only "
        "and is not financial advice."
    )

    return {"explanation": explanation}
