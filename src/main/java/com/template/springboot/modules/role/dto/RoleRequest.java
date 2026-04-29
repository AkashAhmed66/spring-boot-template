package com.template.springboot.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RoleRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "must be UPPER_SNAKE_CASE")
        String name,
        @Size(max = 255) String description,
        Set<String> permissions) {
}
