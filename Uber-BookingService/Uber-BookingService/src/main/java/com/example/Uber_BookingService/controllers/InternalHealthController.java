package com.example.Uber_BookingService.controllers;

import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/health")
public class InternalHealthController {
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final Environment env;

    public InternalHealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate, Environment env) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.env = env;
    }

    @GetMapping("/flow")
    public ResponseEntity<Map<String, Object>> flow() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", "Uber-BookingService");
        payload.put("timestamp", Instant.now().toString());
        payload.put("db", checkDb());
        payload.put("redisLock", checkRedis());
        payload.put("kafkaBootstrapServers", env.getProperty("spring.kafka.bootstrap-servers", "n/a"));

        Map<String, Object> rider = new LinkedHashMap<>();
        rider.put("open", true);
        rider.put("search", true);
        rider.put("select", true);
        rider.put("book", true);
        rider.put("track", true);
        rider.put("pay", true);
        rider.put("rate", true);

        Map<String, Object> driver = new LinkedHashMap<>();
        driver.put("online", true);
        driver.put("accept", true);
        driver.put("pickup", true);
        driver.put("start", true);
        driver.put("drive", true);
        driver.put("end", true);
        driver.put("earn", true);

        Map<String, Object> connections = new LinkedHashMap<>();
        connections.put("book_to_driver_request", true);
        connections.put("accept_to_passenger_visibility", true);
        connections.put("start_to_tracking", true);
        connections.put("end_to_payment_and_ratings", true);

        payload.put("riderFlow", rider);
        payload.put("driverFlow", driver);
        payload.put("flowConnections", connections);
        payload.put("status", Boolean.TRUE.equals(payload.get("db")) ? "UP" : "DEGRADED");
        return ResponseEntity.ok(payload);
    }

    private boolean checkDb() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean checkRedis() {
        String key = "urbanlift:health:" + UUID.randomUUID();
        try {
            redisTemplate.opsForValue().set(key, "1");
            String value = redisTemplate.opsForValue().get(key);
            redisTemplate.delete(key);
            return "1".equals(value);
        } catch (Exception ex) {
            return false;
        }
    }
}
