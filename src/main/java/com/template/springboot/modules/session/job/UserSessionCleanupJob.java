package com.template.springboot.modules.session.job;

import com.template.springboot.modules.session.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSessionCleanupJob {

    @Value("${app.security.sessions.cleanup-retention:P7D}")
    private Duration retention;

    private final UserSessionRepository repository;

    @Scheduled(fixedDelayString = "${app.security.sessions.cleanup-interval:PT1H}",
            initialDelayString = "${app.security.sessions.cleanup-interval:PT1H}")
    @Transactional
    @CacheEvict(value = "userSessions", allEntries = true)
    public void purgeStale() {
        int removed = repository.deleteExpired(Instant.now().minus(retention));
        if (removed > 0) {
            log.info("Purged {} stale user session row(s)", removed);
        }
    }
}
