package com.template.springboot.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "must be UPPER_SNAKE_CASE")
    private String name;

    @Size(max = 255)
    private String description;
}
