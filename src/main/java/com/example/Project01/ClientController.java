package com.example.Project01;

 // match your actual package

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/clients")
public class ClientController {

    private final ClientBucketRepository repository;

    // Spring automatically hands us the repository here — this is called "dependency injection"
    public ClientController(ClientBucketRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/{clientId}")
    public ClientBucket createOrUpdateClient(
            @PathVariable String clientId,
            @RequestParam int capacity,
            @RequestParam double refillRate) {

        ClientBucket bucket = new ClientBucket();
        bucket.setClientId(clientId);
        bucket.setCapacity(capacity);
        bucket.setRefillRate(refillRate);
        bucket.setTokens(capacity); // start full
        bucket.setLastRefillEpochMillis(System.currentTimeMillis());

        return repository.save(bucket);
    }

    @GetMapping("/{clientId}")
    public ClientBucket getClient(@PathVariable String clientId) {
        return repository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

    }
    @GetMapping("/stats")
    public java.util.List<ClientBucket> getAllStats() {
        return repository.findAll();
    }
}