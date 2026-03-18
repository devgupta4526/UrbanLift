package com.example.Uber_AuthService.services;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.expiry}")
    private int expiration = 3600;
    @Value("${jwt.secret}")
    private String SECRET;


    public String createToken(Map<String, Object> payload, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        return Jwts.builder()
                .claims(payload)
                .issuedAt(now)
                .expiration(expiryDate)
                .subject(email)
                .signWith(getSignKey())
                .compact();
    }


    public Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }


    public String createToken(String email) {
        return createToken(new HashMap<>(), email);
    }

    public Claims extractAllPayloads(String token) {
        return Jwts
                .parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public <T> T extractClaims(String token,
                               Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllPayloads(token);
        return claimsResolver.apply(claims);
    }

    public Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    public String extractSubject(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean isTokenValid(String token, String email) {
        final String userEmailFetchedFromToken = extractSubject(token);
        return email.equals(userEmailFetchedFromToken) && !isTokenExpired(token);
    }

    public Object extractPayload(String token, String payloadKey) {
        Claims claims = extractAllPayloads(token);
        return claims.get(payloadKey);
    }

}