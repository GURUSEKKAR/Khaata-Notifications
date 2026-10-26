package com.khaata.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates stateless JWTs. Stateless auth is deliberate: the server
 * keeps no session, so the API scales horizontally without sticky sessions or a
 * shared session store — a strong system-design talking point.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMillis;

    public JwtService(
            @Value("${khaata.security.jwt-secret}") String secret,
            @Value("${khaata.security.jwt-expiry-minutes:720}") long expiryMinutes) {
        // HS256 needs a key of at least 256 bits; pad short dev secrets safely.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes.length >= 32 ? bytes : padTo32(bytes));
        this.expiryMillis = expiryMinutes * 60_000;
    }

    public String issue(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(key)
                .compact();
    }

    /** @return the username (subject) if the token is valid, else null. */
    public String validateAndGetUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] padTo32(byte[] input) {
        byte[] out = new byte[32];
        System.arraycopy(input, 0, out, 0, Math.min(input.length, 32));
        return out;
    }
}
