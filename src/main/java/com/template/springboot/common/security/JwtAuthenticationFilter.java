package com.template.springboot.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_REASON = "auth.failure.reason";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token);
                if (!jwtService.isAccessToken(claims)) {
                    String reason = "Refresh token cannot be used to authenticate API calls — use the access token";
                    log.warn("Rejected non-access token at {} {}", request.getMethod(), request.getRequestURI());
                    request.setAttribute(AUTH_FAILURE_REASON, reason);
                } else {
                    List<SimpleGrantedAuthority> authorities = jwtService.extractAuthorities(claims).stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    Number uid = claims.get("uid", Number.class);
                    Long userId = uid != null ? uid.longValue() : null;
                    AuthenticatedUser principal = new AuthenticatedUser(userId, claims.getSubject());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException ex) {
                String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                log.warn("JWT validation failed at {} {} — {}",
                        request.getMethod(), request.getRequestURI(), reason);
                request.setAttribute(AUTH_FAILURE_REASON, reason);
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
