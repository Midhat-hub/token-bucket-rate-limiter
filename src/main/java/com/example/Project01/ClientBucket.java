package com.example.Project01;




import jakarta.persistence.*;

@Entity
@Table(name = "client_buckets")
public class ClientBucket {

    @Id
    private String clientId;

    private int capacity;
    private double refillRate;
    private double tokens;
    private long lastRefillEpochMillis;

    // Getters and setters (needed by Spring/JPA)
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public double getRefillRate() { return refillRate; }
    public void setRefillRate(double refillRate) { this.refillRate = refillRate; }

    public double getTokens() { return tokens; }
    public void setTokens(double tokens) { this.tokens = tokens; }

    public long getLastRefillEpochMillis() { return lastRefillEpochMillis; }
    public void setLastRefillEpochMillis(long lastRefillEpochMillis) { this.lastRefillEpochMillis = lastRefillEpochMillis; }

    private String algorithm = "TOKEN_BUCKET"; // or "SLIDING_WINDOW"

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    private int windowSeconds = 60;
    private int maxRequestsPerWindow = 10;

    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

    public int getMaxRequestsPerWindow() { return maxRequestsPerWindow; }
    public void setMaxRequestsPerWindow(int maxRequestsPerWindow) { this.maxRequestsPerWindow = maxRequestsPerWindow;
        }
    private long totalAllowed = 0;
    private long totalDenied = 0;

    public long getTotalAllowed() { return totalAllowed; }
    public void setTotalAllowed(long totalAllowed) { this.totalAllowed = totalAllowed; }

    public long getTotalDenied() { return totalDenied; }
    public void setTotalDenied(long totalDenied) { this.totalDenied = totalDenied; }


}