package com.template.springboot.common.idempotency;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.common.exception.BadRequestException;
import com.template.springboot.common.exception.ConflictException;
import com.template.springboot.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(50) // Run before AuditableAspect so a replay isn't double-audited as a fresh action.
@Slf4j
public class IdempotencyAspect {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final IdempotencyStore store;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object intercept(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return pjp.proceed();
        }

        String key = request.getHeader(idempotent.headerName());
        if (key == null || key.isBlank()) {
            if (idempotent.required()) {
                throw new BadRequestException("Missing required header: " + idempotent.headerName());
            }
            return pjp.proceed();
        }
        if (key.length() > 128) {
            throw new BadRequestException(idempotent.headerName() + " must be at most 128 characters");
        }

        String userKey = resolveUserKey(request);
        String requestHash = hashRequest(request.getMethod(), request.getRequestURI(), pjp.getArgs(), methodOf(pjp));

        Optional<IdempotencyRecord> existing = store.find(userKey, key);
        if (existing.isPresent()) {
            return handleExisting(existing.get(), requestHash);
        }

        IdempotencyRecord marker;
        try {
            marker = store.insertInProgress(key, userKey, request.getMethod(),
                    truncate(request.getRequestURI(), 500), requestHash);
        } catch (DataIntegrityViolationException race) {
            IdempotencyRecord winner = store.find(userKey, key)
                    .orElseThrow(() -> new ConflictException("Idempotency conflict: concurrent request"));
            return handleExisting(winner, requestHash);
        }

        try {
            Object result = pjp.proceed();
            store.complete(marker.getId(), result);
            return result;
        } catch (Throwable ex) {
            store.discard(marker.getId());
            throw ex;
        }
    }

    private Object handleExisting(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new ConflictException("Idempotency-Key already used with a different request body");
        }
        if (record.getStatus() == IdempotencyRecord.Status.IN_PROGRESS) {
            throw new ConflictException("A request with this Idempotency-Key is still in progress");
        }
        return replay(record);
    }

    private Object replay(IdempotencyRecord record) {
        try {
            Map<String, Object> body = record.getResponseBody() == null
                    ? Map.of()
                    : objectMapper.readValue(record.getResponseBody(), MAP_TYPE);
            ApiResponse replayed = new ApiResponse(body.get("data"));
            replayed.setMessage((String) body.getOrDefault("message", "OK"));
            replayed.setSuccess(Boolean.TRUE.equals(body.getOrDefault("success", Boolean.TRUE)));
            HttpStatus status = record.getStatusCode() == null
                    ? HttpStatus.OK
                    : HttpStatus.valueOf(record.getStatusCode());
            replayed.setStatus(status);
            return replayed;
        } catch (Exception ex) {
            throw new ConflictException("Stored idempotent response is corrupt");
        }
    }

    private String resolveUserKey(HttpServletRequest request) {
        return SecurityUtils.getCurrentUserId()
                .map(id -> "user:" + id)
                .orElseGet(() -> "anon:" + clientIp(request));
    }

    private static String clientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            int comma = header.indexOf(',');
            return (comma > 0 ? header.substring(0, comma) : header).trim();
        }
        return request.getRemoteAddr();
    }

    private String hashRequest(String method, String path, Object[] args, Method handlerMethod) {
        List<Object> payload = new ArrayList<>();
        payload.add(method);
        payload.add(path);
        Class<?>[] paramTypes = handlerMethod.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            if (isFingerprintable(paramTypes[i], args[i])) {
                payload.add(args[i]);
            }
        }
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static boolean isFingerprintable(Class<?> type, Object value) {
        if (value == null) return true;
        if (MultipartFile.class.isAssignableFrom(type)) return false;
        if (jakarta.servlet.ServletRequest.class.isAssignableFrom(type)) return false;
        if (jakarta.servlet.ServletResponse.class.isAssignableFrom(type)) return false;
        if (org.springframework.data.domain.Pageable.class.isAssignableFrom(type)) return false;
        if (org.springframework.validation.BindingResult.class.isAssignableFrom(type)) return false;
        return true;
    }

    private static Method methodOf(ProceedingJoinPoint pjp) {
        return ((MethodSignature) pjp.getSignature()).getMethod();
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
