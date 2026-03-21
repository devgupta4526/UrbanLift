# Uber-LocationService

Geospatial driver location service. Stores driver positions in Redis GEO and returns nearby drivers within a radius.

---

## Overview

| Property | Value |
|----------|-------|
| **Port** | 7777 |
| **Storage** | Redis (GEO) |
| **Eureka** | Registered |
| **Search radius** | 5 km |

---

## Architecture & Implementation

### Component Structure

```
Uber-LocationService/
├── controllers/
│   └── LocationController.java
├── services/
│   ├── LocationService.java
│   └── RedisLocationServiceImpl.java   # Redis GEO implementation
├── configurations/
│   └── RedisConfig.java                # JedisConnectionFactory, RedisTemplate
└── dtos/
    ├── SaveDriverLocationRequestDto.java
    ├── NearbyDriversRequestDto.java
    └── DriverLocationDto.java
```

### Flow

1. **Save driver location:** `POST /api/location/drivers` → `GeoOperations.add()` on key `drivers` with driverId as member, (lat, lng) as point.
2. **Get nearby drivers:** `POST /api/location/nearby/drivers` → `GeoOperations.radius()` with 5 km circle → Map results to `DriverLocationDto`.

### Redis GEO

- **Key:** `drivers`
- **Member:** `driverId` (String)
- **Coordinates:** latitude, longitude
- **Radius:** 5 km (`Metrics.KILOMETERS`)

### Redis Config

- **Connection:** `JedisConnectionFactory` (host localhost, port 6379)
- **Template:** `StringRedisTemplate` (Spring Boot auto-configures from `RedisConnectionFactory`)
- **Note:** `application.yaml` has Redis host/port commented; defaults to localhost:6379

---

## APIs

| Method | Endpoint | Request Body | Response |
|-------|----------|--------------|----------|
| POST | `/api/location/drivers` | `SaveDriverLocationRequestDto` (driverId, latitude, longitude) | `Boolean` (201) |
| POST | `/api/location/nearby/drivers` | `NearbyDriversRequestDto` (latitude, longitude) | `List<DriverLocationDto>` (200) |

### DTOs

**SaveDriverLocationRequestDto:** `driverId` (String), `latitude`, `longitude`

**DriverLocationDto:** `driverId`, `latitude`, `longitude`

---

## Outgoing API Calls

**None.** LocationService is a leaf service; only consumed by BookingService.

---

## Incoming API Calls (Who Calls This)

| Caller | Endpoint | Purpose |
|--------|----------|---------|
| **BookingService** | `/api/location/nearby/drivers` | Get nearby drivers when creating a booking |

**Who saves driver locations?** No service in this repo calls `/api/location/drivers`. A driver app or a separate driver service would need to call it (e.g. from mobile GPS updates).

---

## Known Issues & Gaps

1. **Redis config in YAML:** Host/port commented; relies on defaults. Uncomment and externalize for production.
2. **Jedis vs Lettuce:** Spring Boot 3 prefers Lettuce; Jedis works but consider migrating.
3. **No driver app:** No component in this repo pushes driver locations; needs driver client or DriverService.
4. **Update semantics:** `GeoOperations.add()` with same member overwrites; no TTL — stale drivers remain until overwritten.
5. **No availability filter:** Returns all drivers in radius; does not filter by availability.

---

## Production Readiness Checklist

| Area | Recommendation |
|------|----------------|
| **Redis** | Externalize host/port; use Redis Cluster for HA |
| **Lettuce** | Switch to Lettuce for Spring Boot 3 compatibility |
| **TTL / cleanup** | Add TTL or periodic cleanup for inactive drivers |
| **Availability** | Filter by driver availability (requires Driver entity or cache) |
| **Configurable radius** | Make search radius configurable (e.g. 5–20 km) |
| **Pagination** | Limit/offset for large result sets |
| **Auth** | Validate caller (e.g. API key or JWT from BookingService) |
| **Health** | Redis health check in `/actuator/health` |
| **Tests** | Integration tests with embedded Redis or Testcontainers |
