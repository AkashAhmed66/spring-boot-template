package com.template.springboot.common.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Inject this anywhere — controllers, services, aspects — to access the authenticated user.
 * Reads from {@code SecurityContextHolder} on every call, so it always reflects the current request.
 *
 * <p>Use the {@code get*()} variants when authentication is required (throw 401 if missing).
 * Use the {@code find*()} variants when the user may legitimately be absent (returns {@link Optional}).
 */
@Service
public class CurrentUserService {

    public CustomUserDetails get() {
        return find().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authentication required"));
    }

    public Optional<CustomUserDetails> find() {
        return SecurityUtils.getCurrentUserDetails();
    }

    public Long getId() {
        return findId().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authentication required"));
    }

    public Optional<Long> findId() {
        return SecurityUtils.getCurrentUserId();
    }

    public String getUsername() {
        return findUsername().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authentication required"));
    }

    public Optional<String> findUsername() {
        return SecurityUtils.getCurrentUsername();
    }

    public String getEmail() {
        return get().getEmail();
    }

    public Set<String> getAuthorities() {
        return SecurityUtils.getCurrentAuthorities();
    }

    public boolean hasRole(String role) {
        return SecurityUtils.hasRole(role);
    }

    public boolean hasAuthority(String authority) {
        return SecurityUtils.hasAuthority(authority);
    }

    public boolean isAuthenticated() {
        return SecurityUtils.getCurrentAuthentication().isPresent();
    }
}
