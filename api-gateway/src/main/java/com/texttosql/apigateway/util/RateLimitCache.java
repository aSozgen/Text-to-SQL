package com.texttosql.apigateway.util;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
public class RateLimitCache {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();


    public Bucket resolveBucket(String clientIp, int replenishRate, int burstCapacity) {
        return cache.computeIfAbsent(clientIp, k -> {
            log.debug("Creating new rate limit bucket for IP: {}", clientIp);
            return createBucket(replenishRate, burstCapacity);
        });
    }

    private Bucket createBucket(int replenishRate, int burstCapacity) {
        Bandwidth limit = Bandwidth.classic(
                burstCapacity,
                Refill.intervally(replenishRate, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public void clearAll() {
        log.info("Clearing all rate limit buckets");
        cache.clear();
    }

    public void remove(String clientIp) {
        log.debug("Removing rate limit bucket for IP: {}", clientIp);
        cache.remove(clientIp);
    }

    public int size() {
        return cache.size();
    }
}