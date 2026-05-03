package com.template.springboot.modules.audit.specification;

import com.template.springboot.modules.audit.dto.AuditLogFilter;
import com.template.springboot.modules.audit.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> withFilter(AuditLogFilter f) {
        Specification<AuditLog> spec = Specification.allOf();
        if (f == null) return spec;

        if (notBlank(f.getUsername()))     spec = spec.and((r, q, cb) -> cb.equal(r.get("username"), f.getUsername()));
        if (f.getUserId() != null)         spec = spec.and((r, q, cb) -> cb.equal(r.get("userId"), f.getUserId()));
        if (notBlank(f.getMethod()))       spec = spec.and((r, q, cb) -> cb.equal(r.get("method"), f.getMethod().toUpperCase()));
        if (notBlank(f.getAction()))       spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), f.getAction()));
        if (notBlank(f.getResourceType())) spec = spec.and((r, q, cb) -> cb.equal(r.get("resourceType"), f.getResourceType()));
        if (notBlank(f.getResourceId()))   spec = spec.and((r, q, cb) -> cb.equal(r.get("resourceId"), f.getResourceId()));
        if (f.getStatusCode() != null)     spec = spec.and((r, q, cb) -> cb.equal(r.get("statusCode"), f.getStatusCode()));
        if (notBlank(f.getRequestId()))    spec = spec.and((r, q, cb) -> cb.equal(r.get("requestId"), f.getRequestId()));
        if (notBlank(f.getPath()))         spec = spec.and((r, q, cb) -> cb.like(r.get("path"), "%" + f.getPath() + "%"));
        if (f.getFrom() != null)           spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("timestamp"), f.getFrom()));
        if (f.getTo() != null)             spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("timestamp"), f.getTo()));

        return spec;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
