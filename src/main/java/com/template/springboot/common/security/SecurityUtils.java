package com.template.springboot.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentAuthentication()
                .map(a -> {
                    Object p = a.getPrincipal();
                    if (p instanceof CustomUserDetails cud) return cud.getUsername();
                    if (p instanceof AuthenticatedUser au) return au.username();
                    return a.getName();
                });
    }

    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(p -> p instanceof CustomUserDetails)
                .map(p -> (CustomUserDetails) p);
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .map(p -> {
                    if (p instanceof CustomUserDetails cud) return cud.getId();
                    if (p instanceof AuthenticatedUser au) return au.id();
                    return null;
                });
    }

    public static Set<String> getCurrentAuthorities() {
        return getCurrentAuthentication()
                .map(a -> a.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public static boolean hasAuthority(String authority) {
        return getCurrentAuthorities().contains(authority);
    }

    public static boolean hasRole(String role) {
        return getCurrentAuthorities().contains("ROLE_" + role);
    }
}
