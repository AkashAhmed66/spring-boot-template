package com.template.springboot.modules.role.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AssignPermissionsRequest(@NotNull Set<String> permissions) {
}
