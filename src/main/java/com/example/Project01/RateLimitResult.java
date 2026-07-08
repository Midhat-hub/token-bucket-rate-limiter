package com.example.Project01;
 // match your actual package

public class RateLimitResult {

    private final boolean allowed;
    private final int limit;
    private final int remaining;
    private final long resetEpochMillis;

    public RateLimitResult(boolean allowed, int limit, int remaining, long resetEpochMillis) {
        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.resetEpochMillis = resetEpochMillis;
    }

    public boolean isAllowed() { return allowed; }
    public int getLimit() { return limit; }
    public int getRemaining() { return remaining; }
    public long getResetEpochMillis() { return resetEpochMillis; }
}