# SentinelGrid

A production-grade distributed social interaction engine built on Spring Boot 3.x, PostgreSQL, and Redis. SentinelGrid is designed for concurrency-critical, horizontally scalable deployments where thread safety, atomic consistency, and stateless operation are non-negotiable.

---

## Architecture Decisions

SentinelGrid uses a **layered modular monolith** — not because microservices are unnecessary, but because the domain boundaries are clear enough that each package (`controller`, `service`, `redis`, `scheduler`) can be extracted into an independent service with minimal refactoring. All inter-layer communication is through interfaces, all dependencies are constructor-injected, and no layer knows about the layer above it.

The system is divided into seven explicit layers:

| Layer | Responsibility |
|---|---|
| API Layer | Request ingestion, validation, HTTP semantics |
| Application Service Layer | Orchestration, business rules |
| Domain Layer | Entities, enums, value objects |
| Infrastructure Layer | Config, security, filters |
| Persistence Layer | JPA repositories, PostgreSQL |
| Redis Coordination Layer | Atomic counters, TTL locks, queues |
| Scheduler Layer | Periodic background sweeper |

---

## Redis Concurrency Strategy

The most critical engineering challenge in SentinelGrid is guaranteeing that **no more than 100 bot comments** exist per post, even under 200 simultaneous concurrent requests hitting the same endpoint at the same millisecond.

### The Problem With Naive Approaches

A naive approach would read the current count from Redis, check if it's below 100, then increment. Under concurrent load this creates a classic **check-then-act race condition** — two threads can both read 99, both pass the check, and both increment to 100 and 101 respectively.

### The Solution: Lua Script Atomicity

Redis executes Lua scripts as a **single atomic operation** — no other Redis command can interleave during script execution. SentinelGrid uses a single Lua script (`bot_cap_check.lua`) that:

1. Reads the current bot count for the post
2. Compares it to the configured cap (100)
3. Either increments and returns `1` (reserved) or returns `0` (cap reached)

All three steps happen as one indivisible unit. This is the only correct way to implement a distributed counter cap with Redis.

```lua
local current = tonumber(redis.call('GET', key) or '0')
if current >= cap then return 0 end
redis.call('INCR', key)
return 1
```

The `botCapScript` bean is a pre-loaded `DefaultRedisScript<Long>` whose SHA is cached by Lettuce, meaning the full script body is only sent once — subsequent calls use `EVALSHA` for minimal network overhead.

### Rollback Safety

If the Lua reservation succeeds but the PostgreSQL `INSERT` fails (network timeout, constraint violation, unexpected exception), the service **releases the reservation** by decrementing the Redis counter. This prevents ghost reservations from permanently consuming cap slots.

```
Redis reserve → DB write → [if DB fails] → Redis release
```

---

## Thread Safety Guarantees

- **No shared mutable state** anywhere in the application. All coordination is delegated to Redis.
- **No `synchronized` blocks**, no `ReentrantLock`, no `AtomicInteger` in application code. Java-level locking is node-local and breaks under horizontal scaling.
- **No static mutable collections**. Every request is fully self-contained.
- Spring's `@Transactional` ensures database-level isolation for PostgreSQL operations.
- The `StringRedisTemplate` used throughout is thread-safe by design — Lettuce's connection pool manages concurrent access at the transport layer.

---

## Redis Atomicity Model

| Operation | Mechanism | Guarantee |
|---|---|---|
| Bot cap check + reserve | Lua script (`EVAL`) | Atomic read-compare-write |
| Bot-human cooldown | `SET NX PX` (setIfAbsent with TTL) | Atomic lock acquisition |
| Virality scoring | `INCRBY` | Atomic increment |
| Notification dispatch | `SET NX` → conditional `RPUSH` | Atomic cooldown check |

All Redis key names are built through `RedisKeyBuilder` — no inline string literals anywhere in the codebase. This makes key structure refactoring a single-file change and prevents typo-induced key mismatches.

---

## Why Statelessness Matters

A stateless application can be scaled horizontally by simply adding more instances behind a load balancer — no sticky sessions, no shared memory, no coordination between JVM processes required.

If SentinelGrid used in-memory `HashMap` or `ConcurrentHashMap` to track bot counts, the cap guarantee would only hold within a single JVM. Instance B would have no knowledge of reservations made by Instance A. Under load balancing, the actual bot count could reach `100 × number_of_instances`.

Redis is the **single source of coordination truth** for all transient state. PostgreSQL is the **single source of persistence truth** for all durable state. The application layer is purely functional.

---

## Database Consistency Approach

- **UUID primary keys** on all entities — safe for distributed generation, no sequence contention.
- **Optimistic consistency** for likes — the `post_likes` table has a unique constraint on `(post_id, author_id, author_type)`. Duplicate likes are caught at the database constraint level and mapped to `HTTP 409 Conflict`.
- **Lazy loading** everywhere — no `FetchType.EAGER`. The N+1 problem is avoided by loading only what each operation needs.
- **`@CreationTimestamp`** on all entities — Hibernate sets the timestamp once at insert time; no application-level clock calls.
- **Indexed columns**: `post_id`, `author_id`, `depth_level`, `created_at` — covering the most frequent query patterns.
- **HikariCP** connection pool with tuned `max-pool-size` per environment profile to prevent thread starvation under load.

---

## Scheduler Design

The `NotificationSweeperScheduler` runs every 5 minutes via `@Scheduled(cron = ...)`. Its design principles:

**Idempotency**: The sweeper drains the Redis list for each pending user atomically — `LRANGE` followed by `DEL`. If the sweeper runs twice (e.g., overlapping cron under a misconfigured scheduler), the second run finds empty lists and is a no-op.

**Memory efficiency**: Notifications are not loaded into a `List` and then sorted or grouped in memory. They are processed user-by-user, keeping the heap footprint proportional to the largest single user's pending queue rather than the global total.

**Resilience**: All exceptions are caught and logged — the scheduler never propagates an exception that would kill the scheduling thread. A Redis failure on one user's queue does not prevent processing of other users.

**Cron expression is externalised** to `application.yml` and bound via `@ConfigurationProperties` — the sweep interval can be changed without recompiling.

---

## Scaling Considerations

**Horizontal scaling**: Add more application instances freely. All coordination state (bot caps, cooldowns, notification queues) lives in Redis. All durable state lives in PostgreSQL. Load balancers can distribute requests without sticky sessions.

**Redis scaling**: For very high throughput, the Lua script and `INCRBY` operations can be routed to Redis Cluster. Key design (`post:{id}:bot_count`) uses a natural hash slot — all keys for the same post land on the same Redis shard, preserving atomicity.

**PostgreSQL scaling**: Connection pool sizes are environment-specific. For read-heavy workloads, read replicas can serve future GET endpoints. Write throughput is bounded only by Postgres IOPS and connection pool limits.

**Scheduler scaling**: In a multi-instance deployment, the notification sweeper will run on every instance simultaneously by default. To prevent duplicate processing, a distributed lock (e.g., `ShedLock` or a Redis-based leader election key) should be added as a future enhancement. Current implementation is safe for single-instance deployments.

---

## Failure Handling

| Failure Mode | Behaviour |
|---|---|
| Redis unreachable during bot cap check | `Exception` propagates → `HTTP 500` returned — request is not accepted without guardrail confirmation |
| DB write fails after Redis reservation | Redis counter is decremented in `catch` block |
| Bot post owner resolution fails | Bot-human cooldown is released before throwing |
| Scheduler Redis failure | Exception caught, logged at ERROR, sweep continues for other users |
| Duplicate like | Caught by pre-check and `DataIntegrityViolationException` → `HTTP 409` |
| Unknown `AuthorType` | `ResourceNotFoundException` → `HTTP 404` |
| Oversized payload | `PayloadSizeFilter` rejects before entering Spring MVC → `HTTP 413` |

---

## Local Setup

**Prerequisites**: Docker, Docker Compose, Java 17, Maven 3.9+

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL on port `5432` and Redis on port `6379`. Both have health checks — wait for `healthy` status before starting the application.

### 2. Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Or build and run the JAR:

```bash
./mvnw clean package -DskipTests
java -jar target/sentinelgrid-1.0.0.jar --spring.profiles.active=local
```

### 3. Seed test data

Create a user:
```bash
# Insert directly via psql — no user management API is in scope for v1
docker exec -it sentinelgrid-postgres psql -U sentinel -d sentinelgrid \
  -c "INSERT INTO users (id, username, is_premium) VALUES (gen_random_uuid(), 'alice', false) RETURNING id;"
```

Create a bot similarly, then use the returned UUIDs in API requests.

### 4. Run tests

```bash
# Unit tests only (no containers required)
./mvnw test -Dgroups='!concurrency'

# All tests including concurrency and integration (Docker required)
./mvnw verify
```

### 5. Actuator health check

```
GET http://localhost:8080/actuator/health
```

---

## Environment Variables (Docker / Production)

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL for PostgreSQL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port (default 6379) |
| `REDIS_PASSWORD` | Redis auth password |

---

## Key Design Constants

| Constant | Value | Location |
|---|---|---|
| Bot reply cap per post | 100 | `sentinelgrid.redis.bot-cap` |
| Bot-human cooldown TTL | 10 minutes | `sentinelgrid.redis.cooldown-ttl-minutes` |
| Notification cooldown | 15 minutes | `sentinelgrid.redis.notification-cooldown-minutes` |
| Max comment depth | 20 | `CommentServiceImpl.MAX_DEPTH` |
| Max request payload | 10 KB | `sentinelgrid.security.max-payload-bytes` |
| Sweeper frequency | Every 5 minutes | `sentinelgrid.scheduler.notification-sweeper-cron` |
