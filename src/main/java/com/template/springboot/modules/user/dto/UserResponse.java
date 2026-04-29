package com.template.springboot.modules.user.dto;

import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.user.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy) {

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.isEnabled(),
                u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getCreatedBy(),
                u.getUpdatedBy());
    }
}
