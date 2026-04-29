package com.template.springboot.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 150) String email,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        Boolean enabled) {
}
