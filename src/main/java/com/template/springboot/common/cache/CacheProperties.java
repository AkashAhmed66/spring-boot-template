package com.template.springboot.common.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-cache configuration. The values here are backend-agnostic so the same yml
 * applies whether the active cache provider is Caffeine, Redis, or anything else
 * that honours TTL semantics.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private Spec defaults = new Spec();

    private Map<String, Spec> caches = new HashMap<>();

    public Spec specFor(String name) {
        return caches.getOrDefault(name, defaults);
    }

    @Getter
    @Setter
    public static class Spec {
        private Duration ttl = Duration.ofMinutes(10);
        private long maxSize = 10_000;
    }
}
