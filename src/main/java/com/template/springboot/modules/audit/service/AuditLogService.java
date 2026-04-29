package com.template.springboot.modules.audit.service;

import com.template.springboot.modules.audit.entity.AuditLog;
import com.template.springboot.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogService {

    private final AuditLogRepository repository;

    /**
     * Persists an audit entry on a dedicated executor in a fresh transaction so a request's own
     * rollback does not erase the trail. Failures here must never bubble back to the caller.
     */
    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log for {} {}: {}",
                    entry.getMethod(), entry.getPath(), ex.getMessage());
        }
    }
}
