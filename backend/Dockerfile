# Use official lightweight Python image
FROM python:3.11-slim

# Set environment variables for Python in container
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8080

WORKDIR /app

# Install runtime dependencies (e.g. curl for health probes)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy and install Python dependencies (layer caching)
COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . /app

# Create non-root user and assign permissions
RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

# Execute uvicorn; exec ensures process receives SIGTERM for graceful shutdown
CMD exec uvicorn main:app --host 0.0.0.0 --port ${PORT}
