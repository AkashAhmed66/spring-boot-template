package com.template.springboot.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory bucket cache, one bucket per (scope + identity) key.
 *
 * <p>For multi-instance deployments, replace the map with a Redis-backed Bucket4j proxy
 * (bucket4j-redis) — the filter API stays identical.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitService {

    private final RateLimitProperties properties;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveDefault(String identity) {
        return buckets.computeIfAbsent("default::" + identity, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.getCapacity())
                        .refillIntervally(properties.getRefillTokens(), properties.getRefillPeriod())
                        .build())
                .build());
    }

    public Bucket resolveAuth(String identity) {
        return buckets.computeIfAbsent("auth::" + identity, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.getAuthCapacity())
                        .refillIntervally(properties.getAuthRefillTokens(), properties.getAuthRefillPeriod())
                        .build())
                .build());
    }
}
