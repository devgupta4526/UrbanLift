package com.example.Uber_BookingService.locking;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LocalLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public LockHandle acquire(String key, Duration wait) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        try {
            boolean acquired = lock.tryLock(Math.max(wait.toMillis(), 1L), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new IllegalStateException("System is busy. Please retry booking in a moment.");
            }
            return new LockHandle(key, UUID.randomUUID().toString(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition interrupted");
        }
    }

    public void release(LockHandle handle) {
        ReentrantLock lock = locks.get(handle.key());
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
