package com.template.springboot.modules.user.dto;

public record UserFilter(String q, String role, Boolean enabled) {
}
