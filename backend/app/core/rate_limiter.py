import time
from collections import defaultdict
from fastapi import Request, HTTPException, status
from app.core.config import settings

# In-memory sliding window rate limiter
_requests: dict[str, list[float]] = defaultdict(list)

def check_auth_rate_limit(request: Request) -> None:
    """Basic IP rate limiting for sensitive authentication endpoints."""
    if settings.PROVEX_ENV.lower() == "testing":
        return
    client_ip = request.client.host if request.client else "unknown"
    now = time.time()
    window = 60.0  # 1 minute
    max_requests = settings.AUTH_RATE_LIMIT_PER_MINUTE

    # Clean old requests
    timestamps = [t for t in _requests[client_ip] if now - t < window]
    if len(timestamps) >= max_requests:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Too many authentication attempts. Please try again later.",
        )
    timestamps.append(now)
    _requests[client_ip] = timestamps
