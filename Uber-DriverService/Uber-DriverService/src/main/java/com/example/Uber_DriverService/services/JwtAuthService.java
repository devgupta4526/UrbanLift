package com.example.Uber_DriverService.services;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtAuthService {

    @Value("${jwt.expiry:3600}")
    private int expiration;

    @Value("${jwt.secret}")
    private String secret;


    public String createToken(String email) {
        return createToken(Map.of(), email);
    }

    public String createToken(Map<String, Object> extraClaims, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000L);
        return Jwts.builder()
                .claims(extraClaims)
                .signWith(getSignedKey())
                .issuedAt(now)
                .expiration(expiryDate)
                .subject(email)
                .compact();
    }


    private Key getSignedKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, String email) {
        try {
            String subject = extractSubject(token);
            return email.equals(subject) && !isTokenExpired(token);
        /* REMOVED: catch (Exception e) — use JwtException only so programming errors fail loudly in dev/tests. */
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }



    private <T>  T extractClaim(String token, Function<Claims,T> resolver) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSignedKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);

    }
}
