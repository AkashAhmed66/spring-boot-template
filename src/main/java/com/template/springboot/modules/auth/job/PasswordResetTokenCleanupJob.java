package com.template.springboot.modules.auth.job;

import com.template.springboot.modules.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetTokenCleanupJob {

    private final PasswordResetTokenRepository repository;

    @Scheduled(fixedDelayString = "${app.mail.password-reset-cleanup-interval:PT1H}",
            initialDelayString = "${app.mail.password-reset-cleanup-interval:PT1H}")
    @Transactional
    public void purge() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(1));
        int removed = repository.deleteExpired(cutoff);
        if (removed > 0) {
            log.info("Purged {} password-reset token row(s)", removed);
        }
    }
}
