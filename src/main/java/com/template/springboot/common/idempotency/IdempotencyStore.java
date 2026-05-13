package com.template.springboot.common.idempotency;

import com.template.springboot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyStore {

    private final IdempotencyRecordRepository repository;
    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> find(String userKey, String key) {
        return repository.findByUserKeyAndIdempotencyKey(userKey, key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord insertInProgress(String key, String userKey, String method,
                                              String path, String requestHash) {
        Instant now = Instant.now();
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setUserKey(userKey);
        record.setMethod(method);
        record.setPath(path);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyRecord.Status.IN_PROGRESS);
        record.setCreatedAt(now);
        record.setExpiresAt(now.plus(properties.getTtl()));
        return repository.saveAndFlush(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long id, Object result) {
        repository.findById(id).ifPresent(record -> {
            record.setStatus(IdempotencyRecord.Status.COMPLETED);
            record.setCompletedAt(Instant.now());
            if (result instanceof ApiResponse api) {
                record.setStatusCode(api.getStatus() == null ? HttpStatus.OK.value() : api.getStatus().value());
                try {
                    record.setResponseBody(objectMapper.writeValueAsString(api));
                } catch (Exception ex) {
                    log.warn("Failed to serialize idempotent response: {}", ex.getMessage());
                }
            } else {
                record.setStatusCode(HttpStatus.OK.value());
                log.warn("@Idempotent method returned non-ApiResponse type {} — replay will return empty body",
                        result == null ? "null" : result.getClass().getName());
            }
            repository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void discard(Long id) {
        repository.deleteById(id);
    }
}
