package com.template.springboot.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class IdempotencyCleanupJob {

    private final IdempotencyRecordRepository repository;

    @Scheduled(fixedDelayString = "#{@idempotencyProperties.cleanupInterval.toMillis()}",
            initialDelayString = "#{@idempotencyProperties.cleanupInterval.toMillis()}")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Purged {} expired idempotency record(s)", removed);
        }
    }
}
