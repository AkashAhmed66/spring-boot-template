package com.template.springboot.common.security;

import com.template.springboot.modules.session.dto.SessionContext;
import com.template.springboot.modules.session.service.UserSessionService;
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
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_REASON = "auth.failure.reason";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserSessionService userSessionService;

    public JwtAuthenticationFilter(JwtService jwtService, UserSessionService userSessionService) {
        this.jwtService = jwtService;
        this.userSessionService = userSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token);
                String rejection = validate(claims);
                if (rejection != null) {
                    log.warn("Rejected token at {} {} — {}", request.getMethod(), request.getRequestURI(), rejection);
                    request.setAttribute(AUTH_FAILURE_REASON, rejection);
                } else {
                    authenticate(claims, request);
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

    private String validate(Claims claims) {
        if (!jwtService.isAccessToken(claims)) {
            return "Refresh token cannot be used to authenticate API calls — use the access token";
        }
        Long userId = jwtService.extractUserId(claims);
        Long sessionId = jwtService.extractSessionId(claims);
        if (userId == null || sessionId == null) {
            return "Token is missing required claims";
        }
        SessionContext session = userSessionService.findContext(sessionId);
        if (session == null) {
            return "Session no longer exists";
        }
        if (!Objects.equals(session.userId(), userId)) {
            return "Session does not belong to this user";
        }
        if (!session.isActive()) {
            return session.revokedAt() != null ? "Session has been revoked" : "Session has expired";
        }
        return null;
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = jwtService.extractAuthorities(claims).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        AuthenticatedUser principal = new AuthenticatedUser(
                jwtService.extractUserId(claims),
                claims.getSubject(),
                jwtService.extractSessionId(claims),
                jwtService.extractImpersonatedBy(claims));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
