package com.template.springboot.modules.role.dto;

import com.template.springboot.modules.permission.entity.Permission;
import com.template.springboot.modules.role.entity.Role;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(), role.getName(), role.getDescription(),
                role.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet()),
                role.getCreatedAt(), role.getUpdatedAt(),
                role.getCreatedBy(), role.getUpdatedBy());
    }
}
