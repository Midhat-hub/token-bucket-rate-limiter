package com.example.Project01;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitService {

    private final ClientBucketRepository repository;
    private final RequestLogRepository requestLogRepository;

    public RateLimitService(ClientBucketRepository repository, RequestLogRepository requestLogRepository) {
        this.repository = repository;
        this.requestLogRepository = requestLogRepository;
    }
    @Transactional
    public RateLimitResult tryConsume(String clientId) {
        ClientBucket bucket = repository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if ("SLIDING_WINDOW".equals(bucket.getAlgorithm())) {
            return trySlidingWindow(bucket);
        } else {
            return tryTokenBucket(bucket);
        }
    }
    private RateLimitResult tryTokenBucket(ClientBucket bucket) {
        long now = System.currentTimeMillis();
        double secondsElapsed = (now - bucket.getLastRefillEpochMillis()) / 1000.0;

        double newTokens = Math.min(
                bucket.getCapacity(),
                bucket.getTokens() + secondsElapsed * bucket.getRefillRate()
        );

        bucket.setLastRefillEpochMillis(now);

        boolean allowed;
        if (newTokens >= 1) {
            bucket.setTokens(newTokens - 1);
            allowed = true;
        } else {
            bucket.setTokens(newTokens);
            allowed = false;
        }

        if (allowed) {
            bucket.setTotalAllowed(bucket.getTotalAllowed() + 1);
        } else {
            bucket.setTotalDenied(bucket.getTotalDenied() + 1);
        }

        repository.save(bucket);

        int remaining = (int) Math.floor(bucket.getTokens());

        // How long until at least 1 token is available again
        double tokensNeeded = Math.max(0, 1 - bucket.getTokens());
        double secondsUntilNextToken = bucket.getRefillRate() > 0 ? tokensNeeded / bucket.getRefillRate() : 0;
        long resetEpochMillis = now + (long) (secondsUntilNextToken * 1000);

        return new RateLimitResult(allowed, bucket.getCapacity(), remaining, resetEpochMillis);
    }
    private RateLimitResult trySlidingWindow(ClientBucket bucket) {
        long now = System.currentTimeMillis();
        long windowStart = now - (bucket.getWindowSeconds() * 1000L);

        long recentCount = requestLogRepository.countRecentRequests(bucket.getClientId(), windowStart);
        boolean allowed = recentCount < bucket.getMaxRequestsPerWindow();

        if (allowed) {
            RequestLogEntry entry = new RequestLogEntry();
            entry.setClientId(bucket.getClientId());
            entry.setRequestedAtEpochMillis(now);
            requestLogRepository.save(entry);
            recentCount++;
            bucket.setTotalAllowed(bucket.getTotalAllowed() + 1);
        } else {
            bucket.setTotalDenied(bucket.getTotalDenied() + 1);
        }

        repository.save(bucket); // add this line if not already present after this block

        int remaining = (int) Math.max(0, bucket.getMaxRequestsPerWindow() - recentCount);

        // Reset time: when the OLDEST request in the window will age out
        Long oldestTimestamp = requestLogRepository.findOldestTimestamp(bucket.getClientId(), windowStart);
        long resetEpochMillis = (oldestTimestamp != null)
                ? oldestTimestamp + (bucket.getWindowSeconds() * 1000L)
                : now;

        return new RateLimitResult(allowed, bucket.getMaxRequestsPerWindow(), remaining, resetEpochMillis);
    }
}