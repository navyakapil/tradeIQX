from fastapi import FastAPI

app = FastAPI(title="TradeIQX API")


@app.get("/")
def home():
    return {"message": "TradeIQX backend is running!"}


@app.get("/health")
def health_check():
    return {"status": "healthy"}