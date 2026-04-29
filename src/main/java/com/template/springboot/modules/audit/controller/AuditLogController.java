package com.template.springboot.modules.audit.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.common.dto.PageResponse;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.HasPermission;
import com.template.springboot.modules.audit.annotation.Auditable;
import com.template.springboot.modules.audit.dto.AuditLogFilter;
import com.template.springboot.modules.audit.dto.AuditLogResponse;
import com.template.springboot.modules.audit.repository.AuditLogRepository;
import com.template.springboot.modules.audit.specification.AuditLogSpecifications;
import com.template.springboot.modules.permission.enums.PermissionName;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.audit", name = "expose-api", havingValue = "true", matchIfMissing = true)
public class AuditLogController {

    private final AuditLogRepository repository;

    @GetMapping
    @HasPermission(PermissionName.AUDIT_READ)
    @Auditable(action = "AUDIT_LIST", resourceType = "AuditLog")
    public ApiResponse list(@org.springframework.web.bind.annotation.RequestParam(required = false) String username,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) Long userId,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String method,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String path,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String action,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String resourceType,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String resourceId,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer statusCode,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String requestId,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) Instant from,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) Instant to,
                            @ParameterObject Pageable pageable) {

        AuditLogFilter filter = new AuditLogFilter(username, userId, method, path, action,
                resourceType, resourceId, statusCode, requestId, from, to);

        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogResponse> page = repository
                .findAll(AuditLogSpecifications.withFilter(filter), effective)
                .map(AuditLogResponse::from);

        return new ApiResponse(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    @HasPermission(PermissionName.AUDIT_READ)
    @Auditable(action = "AUDIT_READ", resourceType = "AuditLog", resourceId = "#id")
    public ApiResponse getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(AuditLogResponse::from)
                .map(ApiResponse::new)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog not found: " + id));
    }
}
