package com.example.Project01;
// match your actual package

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/check/{clientId}")
    public ResponseEntity<String> check(@PathVariable String clientId) {
        RateLimitResult result = rateLimitService.tryConsume(clientId);

        String body = result.isAllowed() ? "ALLOW" : "DENY";
        int status = result.isAllowed() ? 200 : 429;

        return ResponseEntity.status(status)
                .header("X-RateLimit-Limit", String.valueOf(result.getLimit()))
                .header("X-RateLimit-Remaining", String.valueOf(result.getRemaining()))
                .header("X-RateLimit-Reset", String.valueOf(result.getResetEpochMillis() / 1000)) // seconds, standard convention
                .body(body);
    }
}
