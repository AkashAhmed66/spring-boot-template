package com.template.springboot.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache wiring. The provider is selected by {@code spring.cache.type}:
 *
 * <ul>
 *   <li>{@code caffeine} (default) — in-memory, the bean below.</li>
 *   <li>{@code redis} — add {@code spring-boot-starter-data-redis} to the pom and set
 *       {@code spring.cache.type=redis}. Spring Boot auto-configures a {@code RedisCacheManager};
 *       this bean is conditionally skipped so no code change is required to swap.</li>
 *   <li>{@code none} — caching disabled.</li>
 * </ul>
 *
 * Per-cache TTLs are read from {@link CacheProperties} (yml: {@code app.cache.*}).
 */
@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
    CacheManager caffeineCacheManager(CacheProperties properties) {
        SimpleCacheManager manager = new SimpleCacheManager();
        List<CaffeineCache> caches = new ArrayList<>();
        properties.getCaches().forEach((name, spec) -> caches.add(buildCache(name, spec)));
        manager.setCaches(caches);
        return manager;
    }

    private static CaffeineCache buildCache(String name, CacheProperties.Spec spec) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(spec.getMaxSize())
                .recordStats();
        if (spec.getTtl() != null && !spec.getTtl().isZero() && !spec.getTtl().isNegative()) {
            builder.expireAfterWrite(spec.getTtl());
        }
        return new CaffeineCache(name, builder.build());
    }
}
