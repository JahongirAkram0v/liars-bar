package com.example.liars_bar.bot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Har bir foydalanuvchi uchun token bucket: tugmalarni ketma-ket bosib botni
 * (va boshqa o'yinchilarga ketadigan xabarlarni) to'ldirib yuborishning oldini oladi.
 */
final class UserThrottle {

    private static final int CLEANUP_THRESHOLD = 10_000;
    private static final long IDLE_NANOS = 10L * 60 * 1_000_000_000L;

    private final int capacity;
    private final double refillPerNano;
    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    UserThrottle(int capacity, int refillPerSecond) {
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000.0;
    }

    boolean tryAcquire(long userId) {
        long now = System.nanoTime();
        if (buckets.size() > CLEANUP_THRESHOLD) {
            buckets.values().removeIf(b -> now - b.updatedAt > IDLE_NANOS);
        }
        Bucket bucket = buckets.computeIfAbsent(userId, id -> new Bucket(capacity, now));
        synchronized (bucket) {
            bucket.tokens = Math.min(capacity, bucket.tokens + (now - bucket.updatedAt) * refillPerNano);
            bucket.updatedAt = now;
            if (bucket.tokens < 1) {
                return false;
            }
            bucket.tokens -= 1;
            return true;
        }
    }

    private static final class Bucket {
        double tokens;
        long updatedAt;

        Bucket(double tokens, long updatedAt) {
            this.tokens = tokens;
            this.updatedAt = updatedAt;
        }
    }
}
