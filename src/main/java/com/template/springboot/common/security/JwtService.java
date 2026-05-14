package com.template.springboot.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CLAIM_IMPERSONATED_BY = "imp";
    private static final String CLAIM_USER_ID = "uid";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = Base64.getDecoder().decode(props.secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String subject, Long userId, Long sessionId,
                                      List<String> authorities, Long impersonatedBy) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(props.accessTokenTtlMinutes()));
        Map<String, Object> claims = baseClaims(userId, sessionId, TYPE_ACCESS, impersonatedBy);
        claims.put(CLAIM_AUTHORITIES, authorities);
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String subject, Long userId, Long sessionId, Long impersonatedBy) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofDays(props.refreshTokenTtlDays()));
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(subject)
                .claims(baseClaims(userId, sessionId, TYPE_REFRESH, impersonatedBy))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(Claims claims) {
        Object raw = claims.get(CLAIM_AUTHORITIES);
        return raw instanceof List<?> list ? (List<String>) list : List.of();
    }

    public Long extractUserId(Claims claims) {
        Number uid = claims.get(CLAIM_USER_ID, Number.class);
        return uid != null ? uid.longValue() : null;
    }

    public Long extractSessionId(Claims claims) {
        Number sid = claims.get(CLAIM_SESSION_ID, Number.class);
        return sid != null ? sid.longValue() : null;
    }

    public Long extractImpersonatedBy(Claims claims) {
        Number imp = claims.get(CLAIM_IMPERSONATED_BY, Number.class);
        return imp != null ? imp.longValue() : null;
    }

    public long getAccessTokenTtlSeconds() {
        return Duration.ofMinutes(props.accessTokenTtlMinutes()).toSeconds();
    }

    public Duration getRefreshTokenTtl() {
        return Duration.ofDays(props.refreshTokenTtlDays());
    }

    private static Map<String, Object> baseClaims(Long userId, Long sessionId, String type, Long impersonatedBy) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_SESSION_ID, sessionId);
        claims.put(CLAIM_TOKEN_TYPE, type);
        if (impersonatedBy != null) {
            claims.put(CLAIM_IMPERSONATED_BY, impersonatedBy);
        }
        return claims;
    }
}
