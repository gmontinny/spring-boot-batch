package br.com.gmontinny.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS  = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Gera access token com expiração curta (15 min). */
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), TOKEN_TYPE_ACCESS, expirationMs);
    }

    /** Gera refresh token com expiração longa (7 dias). */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), TOKEN_TYPE_REFRESH, refreshExpirationMs);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, c -> c.get(CLAIM_TYPE, String.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            return extractUsername(token).equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && TOKEN_TYPE_ACCESS.equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            return !isTokenExpired(token)
                    && TOKEN_TYPE_REFRESH.equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private String buildToken(String subject, String type, long ttlMs) {
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of(CLAIM_TYPE, type))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(signingKey())
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
