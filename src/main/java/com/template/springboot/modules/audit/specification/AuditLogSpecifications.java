package com.template.springboot.modules.audit.specification;

import com.template.springboot.modules.audit.dto.AuditLogFilter;
import com.template.springboot.modules.audit.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> withFilter(AuditLogFilter f) {
        Specification<AuditLog> spec = Specification.allOf();
        if (f == null) return spec;

        if (notBlank(f.username()))     spec = spec.and((r, q, cb) -> cb.equal(r.get("username"), f.username()));
        if (f.userId() != null)         spec = spec.and((r, q, cb) -> cb.equal(r.get("userId"), f.userId()));
        if (notBlank(f.method()))       spec = spec.and((r, q, cb) -> cb.equal(r.get("method"), f.method().toUpperCase()));
        if (notBlank(f.action()))       spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), f.action()));
        if (notBlank(f.resourceType())) spec = spec.and((r, q, cb) -> cb.equal(r.get("resourceType"), f.resourceType()));
        if (notBlank(f.resourceId()))   spec = spec.and((r, q, cb) -> cb.equal(r.get("resourceId"), f.resourceId()));
        if (f.statusCode() != null)     spec = spec.and((r, q, cb) -> cb.equal(r.get("statusCode"), f.statusCode()));
        if (notBlank(f.requestId()))    spec = spec.and((r, q, cb) -> cb.equal(r.get("requestId"), f.requestId()));
        if (notBlank(f.path()))         spec = spec.and((r, q, cb) -> cb.like(r.get("path"), "%" + f.path() + "%"));
        if (f.from() != null)           spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("timestamp"), f.from()));
        if (f.to() != null)             spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("timestamp"), f.to()));

        return spec;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
