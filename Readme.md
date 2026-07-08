# Token Bucket Rate Limiter Service

A standalone rate-limiting API. Instead of embedding rate-limiting logic inside your app, you run this as its own service and call it over HTTP whenever you need to check "is this client allowed to do this right now?"

Built with Spring Boot + PostgreSQL. Supports two algorithms (token bucket and sliding window), persists state across restarts, and is safe under concurrent requests for the same client.

## How it works

Every client (a user, an API key, another service — whatever you want to rate-limit) gets its own record tracking their usage. When a request comes in asking "can this client proceed?", the service checks that record and returns **ALLOW** or **DENY**.

Two algorithms are available, chosen per client:

**Token bucket** — each client has a bucket that holds a max number of tokens (the burst limit). Every allowed request spends one token. Tokens refill at a steady rate over time, up to the max. This lets clients burst up to their limit all at once, then has to wait for tokens to trickle back in.

**Sliding window** — each client has a rolling time window (e.g. the last 60 seconds) and a max number of requests allowed inside it. The service counts how many requests happened in that window; once the count hits the max, new requests are denied until old ones age out of the window. This is stricter and smoother than token bucket — no saved-up bursts.

State lives in PostgreSQL, not in memory, so it survives restarts and works correctly even if you run multiple instances of the service behind a load balancer, all sharing the same database. Concurrent requests for the same client are handled safely using row-level database locking, so simultaneous requests can't double-spend the same allowance.

## Quick start

**Requirements:** Java 21+, PostgreSQL, Maven (or the bundled `mvnw` wrapper)

1. **Create a database:**
   ```sql
   CREATE DATABASE ratelimiter;
   ```

2. **Configure the connection** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/ratelimiter
   spring.datasource.username=postgres
   spring.datasource.password=yourpassword
   spring.jpa.hibernate.ddl-auto=update
   ```

3. **Run it:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The service starts on `http://localhost:8080`.

## Using it

### 1. Register a client

Before checking a client, tell the service what limits they should have:

```bash
curl -X POST "http://localhost:8080/admin/clients/my-app?capacity=100&refillRate=5"
```

This creates a client called `my-app` with a token bucket that holds up to 100 tokens and refills at 5 tokens/second.

### 2. Check if a request is allowed

Before your app does something you want rate-limited, call:

```bash
curl -i -X POST http://localhost:8080/check/my-app
```

You'll get back either:

```
HTTP/1.1 200
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1783487782

ALLOW
```

or, once the limit is hit:

```
HTTP/1.1 429
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1783487800

DENY
```

Your application should treat `200` as "go ahead" and `429` as "reject/queue this action" — the same convention real-world rate-limited APIs (GitHub, Stripe, Twitter/X, etc.) use.

### 3. Check current status

```bash
curl http://localhost:8080/admin/clients/my-app
```

Returns the client's current token count, capacity, refill rate, and algorithm.

### 4. Switch a client to sliding-window mode

```sql
UPDATE client_buckets
SET algorithm = 'SLIDING_WINDOW', window_seconds = 60, max_requests_per_window = 20
WHERE client_id = 'my-app';
```

(This is set directly via SQL for now — see [Limitations](#limitations).)

### 5. View live stats

```bash
curl http://localhost:8080/admin/clients/stats
```

Returns every client's current state plus running totals of allowed/denied requests. Or open a browser to:

```
http://localhost:8080/dashboard.html
```

for a live-updating table of every client's stats, refreshing every 2 seconds.

## Integrating with your own app

Call `/check/{clientId}` from your application before performing whatever action you want rate-limited — for example, in a middleware/interceptor that runs before your real request handler:

```
1. Incoming request to your app for client "user_123"
2. Your app calls POST http://ratelimiter:8080/check/user_123
3. If 200 → proceed with the real request
4. If 429 → reject the request, optionally passing along the X-RateLimit-* headers
```

Since this is a separate networked service, multiple different apps/services can all point at the same rate limiter and share consistent, centrally-managed limits — rather than each maintaining its own separate (and possibly inconsistent) limiting logic.

## Running multiple instances (distributed mode)

Since state lives in PostgreSQL rather than memory, you can run several instances of this service pointed at the same database, and they'll enforce limits correctly together — a client can't get extra allowance just by having requests land on different instances.

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Run as many as you need, all pointed at the same `application.properties` database config, behind a load balancer.

## Verified correctness

This isn't assumed — it's tested:

- **Concurrency:** 20 truly simultaneous requests against a client with 10 available tokens returned exactly 10 ALLOW / 10 DENY, with zero double-spending, regardless of how the requests were timed or distributed.
- **Load:** 10,000 concurrent requests across 5 clients (400-token capacity each) returned exactly 2,000 ALLOW / 8,000 DENY / 0 errors, at 578 requests/sec.
- **Distributed mode:** 2,000 requests split across two independently running instances, targeting one shared client with 100-token capacity, returned exactly 100 ALLOW combined across both instances / 0 errors.

## API reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/check/{clientId}` | Returns `ALLOW` (200) or `DENY` (429), with rate-limit headers |
| `POST` | `/admin/clients/{clientId}?capacity=X&refillRate=Y` | Create/update a client's token bucket config |
| `GET` | `/admin/clients/{clientId}` | Fetch a client's current state |
| `GET` | `/admin/clients/stats` | Fetch stats (including allow/deny totals) for every client |
| `GET` | `/dashboard.html` | Live dashboard UI |

## Limitations

- Sliding-window settings (`algorithm`, `windowSeconds`, `maxRequestsPerWindow`) don't yet have a dedicated admin endpoint — set via direct SQL for now.
- No authentication on admin endpoints — anyone who can reach the service can reconfigure any client. Add an API-key or auth layer before using this beyond local/internal testing.
- Sliding window logs one row per request rather than using an approximate counter, which is simpler to reason about but uses more storage for very high-volume clients.

## Tech stack

Spring Boot · Spring Data JPA · PostgreSQL · Java 21 (virtual threads used in the included load-testing tool)