package com.template.springboot.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "must be UPPER_SNAKE_CASE")
    private String name;

    @Size(max = 255)
    private String description;

    private Set<String> permissions;
}
