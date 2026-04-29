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
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = Base64.getDecoder().decode(props.secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String subject, Long userId, List<String> authorities) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(props.accessTokenTtlMinutes()));
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(subject)
                .claims(Map.of(
                        "uid", userId,
                        CLAIM_AUTHORITIES, authorities,
                        CLAIM_TOKEN_TYPE, TYPE_ACCESS))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String subject, Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofDays(props.refreshTokenTtlDays()));
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(subject)
                .claims(Map.of(
                        "uid", userId,
                        CLAIM_TOKEN_TYPE, TYPE_REFRESH))
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

    public long getAccessTokenTtlSeconds() {
        return Duration.ofMinutes(props.accessTokenTtlMinutes()).toSeconds();
    }
}
