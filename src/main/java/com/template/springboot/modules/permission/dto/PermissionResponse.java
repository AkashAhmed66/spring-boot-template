package com.template.springboot.modules.permission.dto;

import com.template.springboot.modules.permission.entity.Permission;

import java.time.Instant;

public record PermissionResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy) {

    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getCreatedAt(), p.getUpdatedAt(),
                p.getCreatedBy(), p.getUpdatedBy());
    }
}
