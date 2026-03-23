package com.example.Uber_API_Gateway.configurations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aligns with domain services: invalid JWTs are treated as {@link JwtException} (not broad {@code Exception}).
 * Error responses use the same JSON shape as {@code GlobalExceptionHandler}: {@code status}, {@code error}, {@code message}.
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (isSecured(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Authorization header must start with Bearer", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);
                try {
                    Claims claims = validateToken(token);
                    String userIdForHeader = resolveUserIdForGatewayHeader(claims);
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", userIdForHeader)
                            .header("X-User-Email", claims.getSubject() != null ? claims.getSubject() : "")
                            .header("X-User-Role", claims.get("role", String.class) != null ? claims.get("role", String.class) : "")
                            .build();
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    /* REMOVED: catch (Exception e) — hid bugs; use JwtException for parse/signature/expiry failures. */
                } catch (JwtException e) {
                    return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange);
        };
    }

    /**
     * Public routes on the Gateway (incoming path before downstream rewrite).
     * REMOVED: only {@code /auth/login} and {@code /auth/register} — they never matched real Auth Service paths,
     * so sign-up/sign-in through {@code /auth/api/v1/auth/...} incorrectly required a Bearer token.
     */
    private boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (path == null) {
            return true;
        }
        final List<String> publicPrefixes = List.of(
                "/auth/api/v1/auth/signup",
                "/auth/api/v1/auth/signin",
                "/driver/api/v1/driver/auth/signup",
                "/driver/api/v1/driver/auth/signin",
                "/driver/api/v1/driver/auth/validate",
                "/auth/login",
                "/auth/register"
        );
        for (String prefix : publicPrefixes) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Prefer numeric {@code passengerId} / {@code driverId} claims; fall back to JWT subject (historically email).
     */
    private static String resolveUserIdForGatewayHeader(Claims claims) {
        Object pid = claims.get("passengerId");
        if (pid instanceof Number) {
            return String.valueOf(((Number) pid).longValue());
        }
        Object did = claims.get("driverId");
        if (did instanceof Number) {
            return String.valueOf(((Number) did).longValue());
        }
        return claims.getSubject() != null ? claims.getSubject() : "";
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", httpStatus.value());
        payload.put("error", httpStatus.getReasonPhrase());
        payload.put("message", message);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    public static class Config {
        // Configuration properties if needed
    }
}
