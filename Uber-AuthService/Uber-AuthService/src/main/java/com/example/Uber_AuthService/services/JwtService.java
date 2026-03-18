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


    private Key  getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }


    public String createToken(String email) {
        return createToken(new HashMap<>(), email);
    }

    private Claims extractAllPayloads(String token){
        return Jwts.
                parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private <T> T extractClaims(String token,
                                Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllPayloads(token);
        return claimsResolver.apply(claims);
    }

    private Date extractExpiration(String token){
        return extractClaims(token, Claims::getExpiration);
    }

    private String extractSubject(String token){
        return extractClaims(token, Claims::getSubject);
    }

    private Boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private Boolean isTokenValid(String token, String email){
        final String userEmailFetchedFromToken = extractSubject(token);
        return email.equals(userEmailFetchedFromToken) && !isTokenExpired(token);
    }

    private Object extractPayload(String token , String payloadKey){
        Claims claims = extractAllPayloads(token);
        return claims.get(payloadKey);
    }


}
