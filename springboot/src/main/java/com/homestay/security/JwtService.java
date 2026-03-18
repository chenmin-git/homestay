package com.homestay.security;

import com.homestay.entity.User;
import com.homestay.enums.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final byte[] secretKey;
    private final long expireHours;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expire-hours}") long expireHours
    ) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expireHours = expireHours;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("uid", user.getId())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
            .signWith(Keys.hmacShaKeyFor(secretKey))
            .compact();
    }

    public JwtUserPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secretKey))
            .build()
            .parseSignedClaims(token)
            .getPayload();
        Long userId = Long.valueOf(String.valueOf(claims.get("uid")));
        RoleType role = RoleType.valueOf(String.valueOf(claims.get("role")));
        return new JwtUserPrincipal(userId, claims.getSubject(), role);
    }
}
