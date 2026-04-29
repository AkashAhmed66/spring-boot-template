package com.template.springboot.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PermissionRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "must be UPPER_SNAKE_CASE")
        String name,
        @Size(max = 255) String description) {
}
