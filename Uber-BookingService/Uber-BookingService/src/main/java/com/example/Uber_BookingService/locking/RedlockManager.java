package com.example.Uber_BookingService.locking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis-first distributed lock with local fallback.
 * Implements the core Redlock pattern semantics for a single Redis deployment:
 * token-based ownership + TTL + compare-and-delete unlock script.
 */
@Component
public class RedlockManager {
    private static final Logger log = LoggerFactory.getLogger(RedlockManager.class);
    private static final long RETRY_SLEEP_MS = 75L;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final LocalLockManager local;

    @Value("${urbanlift.lock.booking-create.prefix:urbanlift:lock:booking:create:}")
    private String keyPrefix;

    @Value("${urbanlift.lock.booking-create.lease-ms:15000}")
    private long leaseMs;

    @Value("${urbanlift.lock.booking-create.wait-ms:3000}")
    private long waitMs;

    public RedlockManager(StringRedisTemplate redis, LocalLockManager local) {
        this.redis = redis;
        this.local = local;
    }

    public LockHandle acquireForPassenger(Long passengerId) {
        String businessKey = keyPrefix + passengerId;
        Duration lease = Duration.ofMillis(Math.max(leaseMs, 2000));
        Duration wait = Duration.ofMillis(Math.max(waitMs, 500));
        return acquire(businessKey, lease, wait);
    }

    public LockHandle acquire(String key, Duration lease, Duration wait) {
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + wait.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                Boolean ok = redis.opsForValue().setIfAbsent(key, token, lease);
                if (Boolean.TRUE.equals(ok)) {
                    return new LockHandle(key, token, true);
                }
            } catch (Exception ex) {
                log.warn("Redis lock unavailable, falling back to local lock: {}", ex.getMessage());
                break;
            }
            sleepQuietly(RETRY_SLEEP_MS);
        }
        return local.acquire(key, wait);
    }

    public void release(LockHandle handle) {
        if (handle == null) return;
        if (handle.redis()) {
            try {
                redis.execute(UNLOCK_SCRIPT, Collections.singletonList(handle.key()), handle.token());
                return;
            } catch (Exception ex) {
                log.warn("Redis unlock failed for key {}: {}", handle.key(), ex.getMessage());
            }
        }
        local.release(handle);
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
