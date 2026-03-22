# UrbanLift Backend — Comprehensive API Testing & Debugging Guide

> **Version:** 1.0  
> **Date:** March 22, 2026  
> **Total REST Endpoints:** 28  
> **Total Microservices:** 8 (+ 1 Shared Entity Library + 1 Eureka Discovery)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Service Registry & Ports](#2-service-registry--ports)
3. [Infrastructure Prerequisites](#3-infrastructure-prerequisites)
4. [Startup Order](#4-startup-order)
5. [API Gateway Configuration](#5-api-gateway-configuration)
6. [Authentication & JWT](#6-authentication--jwt)
7. [Auth Service APIs](#7-auth-service-apis)
8. [Driver Service APIs](#8-driver-service-apis)
9. [Booking Service APIs](#9-booking-service-apis)
10. [Location Service APIs](#10-location-service-apis)
11. [Payment Service APIs](#11-payment-service-apis)
12. [Notification Service APIs](#12-notification-service-apis)
13. [Socket/Kafka Service APIs](#13-socketkafka-service-apis)
14. [Kafka Event Flows](#14-kafka-event-flows)
15. [Inter-Service Communication Map](#15-inter-service-communication-map)
16. [Database Schema Reference](#16-database-schema-reference)
17. [End-to-End Test Scenarios](#17-end-to-end-test-scenarios)
18. [Common Errors & Troubleshooting](#18-common-errors--troubleshooting)

---

## 1. Architecture Overview

UrbanLift is a ride-hailing platform built with **Spring Boot microservices**:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        API Gateway (:8080)                         │
│              JWT Filter · Rate Limiter · Route Rewrite             │
└──────┬──────┬──────┬──────┬──────┬──────┬──────┬───────────────────┘
       │      │      │      │      │      │      │
  ┌────▼──┐ ┌─▼───┐ ┌▼────┐ ┌▼───┐ ┌▼───┐ ┌▼───┐ ┌▼──────────┐
  │ Auth  │ │Book-│ │Dri- │ │Loc-│ │Pay-│ │Noti│ │Socket/   │
  │Service│ │ing  │ │ver  │ │ati-│ │ment│ │fica│ │Kafka     │
  │:7475  │ │:8001│ │:8081│ │on  │ │:8082│ │tion│ │:3002     │
  └───────┘ └──┬──┘ └──┬──┘ │:7777│ └──┬──┘ │:8083│ └────┬─────┘
               │       │    └──┬──┘    │    └──┬──┘      │
               │       │      │       │      │          │
        ┌──────▼───────▼──────▼───────▼──────▼──────────▼──┐
        │               Apache Kafka (:9092)                │
        └──────────────────────────────────────────────────-┘
        ┌──────────────────────────────────────────────────-┐
        │          MySQL — uber_db_local (:3306)            │
        └──────────────────────────────────────────────────-┘
        ┌──────────────────────────────────────────────────-┐
        │               Redis (:6379)                       │
        └──────────────────────────────────────────────────-┘
        ┌──────────────────────────────────────────────────-┐
        │          Eureka Discovery (:8761)                  │
        └──────────────────────────────────────────────────-┘
```

**Shared Entity Library:** `Uber-EntityService` is published as a Maven JAR and consumed by all services via `@EntityScan`.

---

## 2. Service Registry & Ports

| Service | Port | Service Discovery Name | Database | Kafka |
|---|---|---|---|---|
| Eureka Discovery | 8761 | — | — | — |
| API Gateway | 8080 | `Uber-API-Gateway` | — | — |
| Auth Service | 7475 | `Uber-AuthService` | `Uber_DB_local` | — |
| Booking Service | 8001 | `Uber-BookingService` | `uber_db_local` | Yes |
| Driver Service | 8081 | `Uber-DriverService` | `uber_db_local` | — |
| Location Service | 7777 | `Uber-LocationService` | — | — |
| Payment Service | 8082 | `Uber-PaymentService` | `uber_db_local` | Yes |
| Notification Service | 8083 | `Uber-NotificationService` | `uber_db_local` | Yes |
| Socket/Kafka Service | 3002 | `Uber-SocketKafkaService` | `uber_db_local` | Yes |

---

## 3. Infrastructure Prerequisites

Before testing, ensure these are running:

| Component | Default Address | Verification |
|---|---|---|
| **MySQL** | `localhost:3306` | `mysql -u root -proot -e "SHOW DATABASES;"` |
| **Apache Kafka + Zookeeper** | `localhost:9092` | `kafka-topics.sh --list --bootstrap-server localhost:9092` |
| **Redis** | `localhost:6379` | `redis-cli ping` → `PONG` |
| **Database** | `uber_db_local` | `CREATE DATABASE IF NOT EXISTS uber_db_local;` |

### MySQL Setup

```sql
CREATE DATABASE IF NOT EXISTS uber_db_local;
CREATE DATABASE IF NOT EXISTS Uber_DB_local;  -- Auth Service uses different case
```

> **Note:** Auth Service connects to `Uber_DB_local` while other services use `uber_db_local`. Depending on your OS, these may or may not be the same database.

---

## 4. Startup Order

Start services in this order:

1. **MySQL, Kafka, Redis** (infrastructure)
2. **Eureka Discovery** (`Uber-ServiceDiscovery-Eureka`) — wait until dashboard shows at `http://localhost:8761`
3. **Entity Service** — build the JAR: `mvn clean install` in `Uber-EntityService`
4. **Auth Service, Driver Service, Location Service** (no inter-service dependencies at startup)
5. **Booking Service, Payment Service, Notification Service, Socket/Kafka Service** (depend on Kafka + other services)
6. **API Gateway** (last — needs all services registered in Eureka)

---

## 5. API Gateway Configuration

### Route Mapping

All requests go through `http://localhost:8080` and are rewritten:

| Gateway Path | Rewrites To | Target Service |
|---|---|---|
| `/auth/**` | `/{path}` | `lb://Uber-AuthService` |
| `/booking/**` | `/{path}` | `lb://Uber-BookingService` |
| `/driver/**` | `/{path}` | `lb://Uber-DriverService` |
| `/location/**` | `/{path}` | `lb://Uber-LocationService` |
| `/payment/**` | `/{path}` | `lb://Uber-PaymentService` |
| `/notification/**` | `/{path}` | `lb://Uber-NotificationService` |
| `/socket/**` | `/{path}` | `lb://Uber-SocketKafkaService` |

### Rate Limiting

- **Replenish Rate:** 10 requests/sec
- **Burst Capacity:** 20 requests
- **Key Resolver:** `X-User-Id` header or client IP

### JWT Filter

All routes pass through `JwtAuthenticationFilter`. Unsecured paths: `/auth/login`, `/auth/register`.

**Files involved:**
- `Uber-API-Gateway/.../configurations/GatewayConfig.java` — route definitions
- `Uber-API-Gateway/.../configurations/JwtAuthenticationFilter.java` — JWT validation, header injection
- `Uber-API-Gateway/.../configurations/RateLimitConfig.java` — rate limiter key resolver

---

## 6. Authentication & JWT

### JWT Configuration

| Property | Value |
|---|---|
| Secret Key | `undergroundjddkdjkdjododkdjdkldkdldjkdkdljdljdkldjldldjdldjldjldjdldljdld` |
| Expiry | 3600 seconds (1 hour) |
| Algorithm | HMAC-SHA |

### Passenger Auth Flow
1. Sign up → `POST /api/v1/auth/signup/passenger`
2. Sign in → `POST /api/v1/auth/signin/passenger` → sets `JWT_TOKEN` cookie
3. Subsequent requests: send `JWT_TOKEN` cookie or `Authorization: Bearer <token>` header

### Driver Auth Flow
1. Sign up → `POST /api/v1/driver/auth/signup` → driver starts as `PENDING` approval
2. Manually approve driver in DB: `UPDATE driver SET driver_approval_status = 'APPROVED' WHERE id = ?;`
3. Sign in → `POST /api/v1/driver/auth/signin` → sets `DRIVER_JWT` cookie
4. Subsequent requests: send `DRIVER_JWT` cookie or `Authorization: Bearer <token>` header

---

## 7. Auth Service APIs

**Base URL (Direct):** `http://localhost:7475`  
**Base URL (Gateway):** `http://localhost:8080/auth`

---

### 7.1 POST `/api/v1/auth/signup/passenger`

**Purpose:** Register a new passenger account.

**Request Body:**
```json
{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+919876543210",
    "password": "SecurePass123",
    "address": "123 Main St, Delhi"
}
```

**Success Response (201 CREATED):**
```json
{
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+919876543210",
    "address": "123 Main St, Delhi",
    "rating": null,
    "createdAt": "2026-03-22T10:30:00.000+00:00"
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `AuthController` | `signup()` | `Uber-AuthService/.../controllers/AuthController.java` |
| Service | `AuthService` | `signUpPassenger()` | `Uber-AuthService/.../services/AuthService.java` |
| Repository | `PassengerRepository` | `save()` | `Uber-AuthService/.../repositories/PassengerRepository.java` |
| Entity | `Passenger` | — | `Uber-EntityService/.../Models/Passenger.java` |
| DTO (Req) | `PassengerSignUpRequestDto` | — | `Uber-AuthService/.../dto/PassengerSignUpRequestDto.java` |
| DTO (Res) | `PassengerDto` | `from()` | `Uber-AuthService/.../dto/PassengerDto.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - DataIntegrityViolationException` | Duplicate email in `passenger` table | Check if email already exists |
| `500 - JPA error` | `ddl-auto: validate` fails — table schema mismatch | Run Entity Service DDL or set `ddl-auto: update` |
| `500 - BeanCreationException` | `BCryptPasswordEncoder` bean not found | Verify `SpringSecurity.java` config exposes `BCryptPasswordEncoder` bean |
| `404` via Gateway | Auth Service not registered in Eureka | Check Eureka dashboard, verify `spring.application.name` |

---

### 7.2 POST `/api/v1/auth/signin/passenger`

**Purpose:** Authenticate a passenger, receive JWT token via cookie.

**Request Body:**
```json
{
    "email": "john.doe@example.com",
    "password": "SecurePass123"
}
```

**Success Response (200 OK):**
```json
{
    "success": true
}
```
**Response Headers:** `Set-Cookie: JWT_TOKEN=<jwt>; Path=/; Max-Age=604800; HttpOnly`

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `AuthController` | `signin()` | `Uber-AuthService/.../controllers/AuthController.java` |
| Security | `AuthenticationManager` | `authenticate()` | Spring Security (auto-configured) |
| Security | `UserDetailsServiceImpl` | `loadUserByUsername()` | `Uber-AuthService/.../services/UserDetailsServiceImpl.java` |
| Service | `JwtService` | `createToken()` | `Uber-AuthService/.../services/JwtService.java` |
| DTO (Req) | `AuthRequestDto` | — | `Uber-AuthService/.../dto/AuthRequestDto.java` |
| DTO (Res) | `AuthResponseDto` | — | `Uber-AuthService/.../dto/AuthResponseDto.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `403 Forbidden` | CSRF enabled or security misconfiguration | Verify `SpringSecurity.java` disables CSRF for API |
| `401 - UsernameNotFoundException` | Email not found in DB | Verify passenger exists in `passenger` table |
| `401 - BadCredentialsException` | Password mismatch | Ensure password was BCrypt-encoded during signup |
| `500 - SignatureException` | JWT secret key too short (<256 bits) | Verify `jwt.secret` in `application.yaml` is >= 32 chars |

**Important Note:** The current implementation hardcodes `"Abc@gmail"` in `createToken()` call instead of using the actual email. This is a known issue.

---

### 7.3 GET `/api/v1/auth/validate`

**Purpose:** Validate an existing JWT token from cookies.

**Request:** Send `JWT_TOKEN` cookie.

**Success Response (200 OK):**
```json
{
    "success": true
}
```

**Failure Response (401 UNAUTHORIZED):** `"Invalid JWT_TOKEN"` or `"No cookies found"`

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `AuthController` | `validate()` | `Uber-AuthService/.../controllers/AuthController.java` |
| Service | `JwtService` | `extractSubject()`, `isTokenValid()` | `Uber-AuthService/.../services/JwtService.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `401 - "No cookies found"` | Cookie not sent with request | In Postman, add cookie `JWT_TOKEN=<value>` |
| `401 - "Invalid JWT_TOKEN"` | Token expired or secret mismatch | Regenerate token; check `jwt.secret` matches |
| `401 - ExpiredJwtException` | Token expiry reached | Re-authenticate; check `jwt.expiry` value |

---

## 8. Driver Service APIs

**Base URL (Direct):** `http://localhost:8081`  
**Base URL (Gateway):** `http://localhost:8080/driver`

---

### 8.1 POST `/api/v1/driver/auth/signup`

**Purpose:** Register a new driver with vehicle details.

**Request Body:**
```json
{
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "email": "rajesh.driver@example.com",
    "phoneNumber": "+919876543211",
    "password": "DriverPass123",
    "address": "456 Driver Lane, Mumbai",
    "licenseNumber": "DL-1234567890",
    "aadharNumber": "1234-5678-9012",
    "activeCity": "Mumbai",
    "car": {
        "plateNumber": "MH-01-AB-1234",
        "colorName": "White",
        "brand": "Maruti",
        "model": "Swift Dzire",
        "carType": "SEDAN"
    }
}
```

**Valid `carType` values:** `XL`, `SEDAN`, `HATCHBACK`, `COMPACT_SUV`, `SUV`

**Success Response (201 CREATED):**
```json
{
    "id": 1,
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "email": "rajesh.driver@example.com",
    "phoneNumber": "+919876543211",
    "address": "456 Driver Lane, Mumbai",
    "licenseNumber": "DL-1234567890",
    "driverApprovalStatus": "PENDING",
    "activeCity": "Mumbai",
    "isAvailable": false,
    "rating": 0.0,
    "car": {
        "plateNumber": "MH-01-AB-1234",
        "colorName": "White",
        "brand": "Maruti",
        "model": "Swift Dzire",
        "carType": "SEDAN"
    }
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverAuthController` | `signUp()` | `Uber-DriverService/.../controllers/DriverAuthController.java` |
| Service | `DriverAuthService` | `signUp()`, `toDto()` | `Uber-DriverService/.../services/DriverAuthService.java` |
| Repository | `DriverRepository` | `save()` | `Uber-DriverService/.../repositories/DriverRepository.java` |
| Repository | `CarRepository` | `save()` | `Uber-DriverService/.../repositories/CarRepository.java` |
| Repository | `ColorRepository` | `findByName()`, `save()` | `Uber-DriverService/.../repositories/ColorRepository.java` |
| Entity | `Driver`, `Car`, `Color` | — | `Uber-EntityService/.../Models/` |
| DTO (Req) | `DriverSignUpRequestDto` | — | `Uber-DriverService/.../dtos/DriverSignUpRequestDto.java` |
| DTO (Res) | `DriverDto`, `CarDto` | — | `Uber-DriverService/.../dtos/DriverDto.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - DataIntegrityViolationException` | Duplicate `email`, `licenseNumber`, or `aadharNumber` | Check unique constraints in `driver` table |
| `500 - IllegalArgumentException` | `"Driver with email already exists"` | Use a different email |
| `500 - IllegalArgumentException` for CarType | Invalid `carType` value | Use: `XL`, `SEDAN`, `HATCHBACK`, `COMPACT_SUV`, `SUV` |
| `500 - Table doesn't exist` | `color` or `car` table missing | Run Entity Service with `ddl-auto: update` first |

---

### 8.2 POST `/api/v1/driver/auth/signin`

**Purpose:** Authenticate a driver (must be APPROVED).

**Request Body:**
```json
{
    "email": "rajesh.driver@example.com",
    "password": "DriverPass123"
}
```

**Pre-condition:** Driver must have `driver_approval_status = 'APPROVED'` in the database.

```sql
UPDATE driver SET driver_approval_status = 'APPROVED' WHERE email = 'rajesh.driver@example.com';
```

**Success Response (200 OK):**
```json
{
    "success": true
}
```
**Response Headers:** `Set-Cookie: DRIVER_JWT=<jwt>; Path=/; Max-Age=604800; HttpOnly`

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverAuthController` | `signIn()` | `Uber-DriverService/.../controllers/DriverAuthController.java` |
| Repository | `DriverRepository` | `findByEmail()` | `Uber-DriverService/.../repositories/DriverRepository.java` |
| Service | `JwtAuthService` | `createToken()` | `Uber-DriverService/.../services/JwtAuthService.java` |
| Security | `PasswordEncoder` | `matches()` | Spring Security |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `401 - success: false` | Driver not APPROVED | Run `UPDATE driver SET driver_approval_status = 'APPROVED' WHERE email = ?;` |
| `401 - success: false` | Wrong password | Verify password matches BCrypt-encoded value |
| `401 - success: false` | Email not found | Verify driver exists via `SELECT * FROM driver WHERE email = ?;` |

---

### 8.3 GET `/api/v1/driver/auth/validate`

**Purpose:** Validate driver JWT token.

**Request:** Send `DRIVER_JWT` cookie or `Authorization: Bearer <token>` header.

**Success Response (200 OK):**
```json
{
    "success": true
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverAuthController` | `validate()` | `Uber-DriverService/.../controllers/DriverAuthController.java` |
| Service | `JwtAuthService` | `extractSubject()`, `isTokenValid()` | `Uber-DriverService/.../services/JwtAuthService.java` |

---

### 8.4 GET `/api/v1/driver/profile`

**Purpose:** Get the authenticated driver's profile.

**Request:** Requires authentication (JWT token).

**Success Response (200 OK):**
```json
{
    "id": 1,
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "email": "rajesh.driver@example.com",
    "phoneNumber": "+919876543211",
    "address": "456 Driver Lane, Mumbai",
    "licenseNumber": "DL-1234567890",
    "driverApprovalStatus": "APPROVED",
    "activeCity": "Mumbai",
    "isAvailable": true,
    "rating": 4.5,
    "car": {
        "plateNumber": "MH-01-AB-1234",
        "colorName": "White",
        "brand": "Maruti",
        "model": "Swift Dzire",
        "carType": "SEDAN"
    }
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverProfileController` | `getProfile()` | `Uber-DriverService/.../controllers/DriverProfileController.java` |
| Service | `DriverAuthService` | `toDto()` | `Uber-DriverService/.../services/DriverAuthService.java` |
| Filter | `JwtAuthFilter` | `doFilterInternal()` | `Uber-DriverService/.../filters/JwtAuthFilter.java` |
| Repository | `DriverRepository` | `findByEmail()` | `Uber-DriverService/.../repositories/DriverRepository.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `401 Unauthorized` | Missing or invalid JWT | Send valid `DRIVER_JWT` cookie or Bearer token |
| `500 - IllegalArgumentException` | `"Driver not found"` | Token email doesn't match any driver in DB |
| `500 - LazyInitializationException` | Lazy loading issue for `car` | Verify `enable_lazy_load_no_trans: true` in config |

---

### 8.5 PUT `/api/v1/driver/availability`

**Purpose:** Set driver availability and optionally update location.

**Request Body:**
```json
{
    "available": true
}
```

**Query Parameters (optional):** `?lat=19.0760&lng=72.8777`

**Requires authentication.**

**Success Response (200 OK):** Empty body

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverAvailabilityController` | `setAvailability()` | `Uber-DriverService/.../controllers/DriverAvailabilityController.java` |
| Service | `DriverAvailabilityService` | `setAvailability()` | `Uber-DriverService/.../services/DriverAvailabilityService.java` |
| Service | `DriverLocationService` | `updateLocation()` | `Uber-DriverService/.../services/DriverLocationService.java` |
| API Client | `LocationServiceApi` (Retrofit) | `saveDriverLocation()` | `Uber-DriverService/.../apis/LocationServiceApi.java` |
| Repository | `DriverRepository` | `findById()`, `save()` | `Uber-DriverService/.../repositories/DriverRepository.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - RuntimeException` | `"Failed to sync location to LocationService"` | Ensure Location Service is running on port 7777 |
| `500 - IllegalArgumentException` | `"Driver not found"` | Token email doesn't match any driver |
| `401` | Missing auth token | Include JWT token in request |

---

### 8.6 POST `/api/v1/driver/location`

**Purpose:** Update driver's current location.

**Request Body:**
```json
{
    "latitude": 19.0760,
    "longitude": 72.8777
}
```

**Requires authentication.**

**Success Response (200 OK):** Empty body

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverLocationController` | `updateLocation()` | `Uber-DriverService/.../controllers/DriverLocationController.java` |
| Service | `DriverLocationService` | `updateLocation()` | `Uber-DriverService/.../services/DriverLocationService.java` |
| API Client | `LocationServiceApi` (Retrofit) | `saveDriverLocation()` | `Uber-DriverService/.../apis/LocationServiceApi.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - RuntimeException` | Location Service unreachable | Verify Location Service on port 7777 |
| `500 - ConnectException` | Retrofit connection refused | Check `RetrofitConfig.java` base URL |

---

## 9. Booking Service APIs

**Base URL (Direct):** `http://localhost:8001`  
**Base URL (Gateway):** `http://localhost:8080/booking`

---

### 9.1 POST `/api/v1/booking`

**Purpose:** Create a new ride booking. Automatically finds nearby drivers and sends ride requests.

**Request Body:**
```json
{
    "passengerId": 1,
    "startLocation": {
        "latitude": 19.0760,
        "longitude": 72.8777
    },
    "endLocation": {
        "latitude": 19.1136,
        "longitude": 72.8697
    }
}
```

**Success Response (201 CREATED):**
```json
{
    "bookingId": 1,
    "bookingStatus": "ASSIGNING_DRIVER",
    "driver": null
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BookingController` | `createBooking()` | `Uber-BookingService/.../controllers/BookingController.java` |
| Service | `BookingServiceImpl` | `createBooking()` | `Uber-BookingService/.../services/BookingServiceImpl.java` |
| Service | `BookingServiceImpl` | `processNearbyDrivers()` | (private, same file) |
| Service | `BookingServiceImpl` | `raiseRideRequestAsync()` | (private, same file) |
| API Client | `LocationServiceApi` (Retrofit) | `getNearbyDrivers()` | `Uber-BookingService/.../apis/LocationServiceApi.java` |
| API Client | `UberSocketApi` (Retrofit) | `raiseRideRequest()` | `Uber-BookingService/.../apis/UberSocketApi.java` |
| Repository | `PassengerRepository` | `findById()` | `Uber-BookingService/.../repositories/PassengerRepository.java` |
| Repository | `BookingRepository` | `save()` | `Uber-BookingService/.../repositories/BookingRepository.java` |
| Entity | `Booking`, `ExactLocation`, `Passenger` | — | `Uber-EntityService/.../Models/` |
| Config | `RetrofitConfig` | Retrofit HTTP client setup | `Uber-BookingService/.../controllers/RetrofitConfig.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - IllegalArgumentException` | `"Passenger not found: X"` | Ensure passenger with given ID exists in DB |
| `500 - RetrofitError` | Location Service unreachable | Verify Location Service on port 7777 |
| `500 - RetrofitError` | Socket Service unreachable | Verify Socket Service on port 3002 |
| `500 - JPA error` | `exact_location` table missing | Run Entity Service DDL |
| Booking created but no driver assigned | No drivers in Redis geo set | Save driver locations first via Location Service |
| `201` but driver never assigned | Async flow failed silently | Check Booking Service logs for Retrofit callback errors |

---

### 9.2 GET `/api/v1/booking/{bookingId}`

**Purpose:** Get booking details by ID.

**Success Response (200 OK):**
```json
{
    "id": 1,
    "bookingStatus": "ASSIGNING_DRIVER",
    "bookingDate": "2026-03-22",
    "startTime": "2026-03-22T10:30:00.000+00:00",
    "endTime": "2026-03-22T10:30:00.000+00:00",
    "totalDistance": 0,
    "passenger": {
        "id": 1,
        "firstName": "John",
        "lastName": "Doe",
        "email": "john.doe@example.com"
    },
    "driver": null,
    "startLocation": {
        "id": 1,
        "latitude": 19.0760,
        "longitude": 72.8777
    },
    "endLocation": {
        "id": 2,
        "latitude": 19.1136,
        "longitude": 72.8697
    }
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BookingController` | `getBooking()` | `Uber-BookingService/.../controllers/BookingController.java` |
| Service | `BookingServiceImpl` | `getBookingById()`, `toDetailDto()` | `Uber-BookingService/.../services/BookingServiceImpl.java` |
| Repository | `BookingRepository` | `findById()` | `Uber-BookingService/.../repositories/BookingRepository.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - IllegalArgumentException` | `"Booking not found: X"` | Verify booking ID exists |
| `500 - LazyInitializationException` | Passenger/Driver lazy load failed | Add `enable_lazy_load_no_trans: true` to config |

---

### 9.3 POST `/api/v1/booking/{bookingId}`

**Purpose:** Update a booking (assign driver, change status).

**Request Body:**
```json
{
    "status": "SCHEDULED",
    "driverId": 1
}
```

**Valid `status` values:** `SCHEDULED`, `CANCELLED`, `CAB_ARRIVED`, `ASSIGNING_DRIVER`, `IN_RIDE`, `COMPLETED`

**Success Response (200 OK):**
```json
{
    "bookingId": 1,
    "status": "SCHEDULED",
    "driver": {
        "id": 1,
        "firstName": "Rajesh",
        "lastName": "Kumar"
    }
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BookingController` | `updateBooking()` | `Uber-BookingService/.../controllers/BookingController.java` |
| Service | `BookingServiceImpl` | `updateBooking()` | `Uber-BookingService/.../services/BookingServiceImpl.java` |
| Repository | `BookingRepository` | `updateBookingStatusAndDriverById()` | `Uber-BookingService/.../repositories/BookingRepository.java` |
| Repository | `DriverRepository` | `findById()` | `Uber-BookingService/.../repositories/DriverRepository.java` |

---

### 9.4 GET `/api/v1/booking/passenger/{passengerId}`

**Purpose:** Get all bookings for a passenger.

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BookingController` | `getBookingsByPassenger()` | `Uber-BookingService/.../controllers/BookingController.java` |
| Service | `BookingServiceImpl` | `getBookingsByPassengerId()` | `Uber-BookingService/.../services/BookingServiceImpl.java` |
| Repository | `BookingRepository` | `findByPassengerId()` | `Uber-BookingService/.../repositories/BookingRepository.java` |

---

### 9.5 GET `/api/v1/booking/driver/{driverId}`

**Purpose:** Get all bookings for a driver.

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BookingController` | `getBookingsByDriver()` | `Uber-BookingService/.../controllers/BookingController.java` |
| Service | `BookingServiceImpl` | `getBookingsByDriverId()` | `Uber-BookingService/.../services/BookingServiceImpl.java` |
| Repository | `BookingRepository` | `findByDriverId()` | `Uber-BookingService/.../repositories/BookingRepository.java` |

---

### 9.6 PUT `/api/v1/booking/{bookingId}/status`

**Purpose:** Update booking status. Triggers notifications and Kafka events.

**Query Parameter:** `?status=COMPLETED`

**Side Effects:**
- Sends notification event via Kafka for: `CAB_ARRIVED`, `IN_RIDE`, `COMPLETED`, `CANCELLED`
- On `COMPLETED`: publishes `BookingCompletedEventDto` to `booking-completed-topic` (consumed by Payment Service)

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BookingController` | `updateStatus()` | `Uber-BookingService/.../controllers/BookingController.java` |
| Service | `BookingServiceImpl` | `updateBookingStatus()` | `Uber-BookingService/.../services/BookingServiceImpl.java` |
| Service | `BookingServiceImpl` | `sendNotificationForStatusChange()` | (private, same file) |
| Producer | `KafkaProducerService` | `sendBookingCompletedEvent()`, `sendNotificationEvent()` | `Uber-BookingService/.../producers/KafkaProducerService.java` |
| Repository | `BookingRepository` | `updateBookingStatusById()` | `Uber-BookingService/.../repositories/BookingRepository.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - IllegalArgumentException` | Invalid status string | Use valid `BookingStatus` enum value |
| `500 - KafkaException` | Kafka broker unreachable | Verify Kafka on `localhost:9092` |
| Status updates but no notification | Kafka topic doesn't exist | Create topic: `kafka-topics.sh --create --topic notification-events-topic --bootstrap-server localhost:9092` |

---

### 9.7 POST `/api/v1/booking/{bookingId}/cancel`

**Purpose:** Cancel a booking (shortcut for setting status to `CANCELLED`).

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BookingController` | `cancelBooking()` | `Uber-BookingService/.../controllers/BookingController.java` |
| Service | `BookingServiceImpl` | `cancelBooking()` → `updateBookingStatus()` | `Uber-BookingService/.../services/BookingServiceImpl.java` |

---

## 10. Location Service APIs

**Base URL (Direct):** `http://localhost:7777`  
**Base URL (Gateway):** `http://localhost:8080/location`

**Storage Backend:** Redis GEO operations (key: `drivers`, radius: 5 km)

---

### 10.1 POST `/api/location/drivers`

**Purpose:** Save/update a driver's GPS location in Redis.

**Request Body:**
```json
{
    "driverId": "1",
    "latitude": 19.0760,
    "longitude": 72.8777
}
```

**Success Response (201 CREATED):**
```json
true
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `LocationController` | `saveDriverLocation()` | `Uber-LocationService/.../controllers/LocationController.java` |
| Service (Interface) | `LocationService` | `saveDriverLocation()` | `Uber-LocationService/.../services/LocationService.java` |
| Service (Impl) | `RedisLocationServiceImpl` | `saveDriverLocation()` | `Uber-LocationService/.../services/RedisLocationServiceImpl.java` |
| Config | `RedisConfig` | Redis template setup | `Uber-LocationService/.../configurations/RedisConfig.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - RedisConnectionFailureException` | Redis not running | Start Redis: `redis-server` |
| `500 - Internal Server Error` | Redis config commented out in `application.yaml` | Uncomment `spring.data.redis.host` and `spring.data.redis.port` |
| `500 - BeanCreationException` | `StringRedisTemplate` not autowired | Verify Redis dependency in `pom.xml` and config |

---

### 10.2 POST `/api/location/nearby/drivers`

**Purpose:** Find drivers within 5 km radius of given coordinates.

**Request Body:**
```json
{
    "latitude": 19.0760,
    "longitude": 72.8777
}
```

**Success Response (200 OK):**
```json
[
    {
        "driverId": "1",
        "latitude": 19.0760,
        "longitude": 72.8777
    },
    {
        "driverId": "2",
        "latitude": 19.0800,
        "longitude": 72.8800
    }
]
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `LocationController` | `getNearbyDrivers()` | `Uber-LocationService/.../controllers/LocationController.java` |
| Service (Interface) | `LocationService` | `getNearbyDrivers()` | `Uber-LocationService/.../services/LocationService.java` |
| Service (Impl) | `RedisLocationServiceImpl` | `getNearbyDrivers()` | `Uber-LocationService/.../services/RedisLocationServiceImpl.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `200` with empty `[]` | No drivers saved in Redis | Save driver locations first via `POST /api/location/drivers` |
| `500 - RedisConnectionFailureException` | Redis offline | Start Redis |
| `500 - NullPointerException` | `geoOperations.radius()` returns null | Verify Redis GEO key `drivers` exists: `redis-cli GEORADIUS drivers 72.8 19.07 10 km` |

---

## 11. Payment Service APIs

**Base URL (Direct):** `http://localhost:8082`  
**Base URL (Gateway):** `http://localhost:8080/payment`

---

### 11.1 POST `/api/v1/fare/estimate`

**Purpose:** Estimate fare for a ride based on start/end coordinates and car type.

**Request Body:**
```json
{
    "startLat": 19.0760,
    "startLng": 72.8777,
    "endLat": 19.1136,
    "endLng": 72.8697,
    "carType": "SEDAN"
}
```

**Success Response (200 OK):**
```json
{
    "estimatedFare": 180.00,
    "baseFare": 50.00,
    "distanceFare": 100.00,
    "timeFare": 0.00,
    "surgeMultiplier": 1.20,
    "totalFare": 180.00
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `FareController` | `estimateFare()` | `Uber-PaymentService/.../controllers/FareController.java` |
| Service | `FareCalculationService` | `estimateFare()`, `calculateDistance()` | `Uber-PaymentService/.../services/FareCalculationService.java` |
| Repository | `FareConfigRepository` | `findByCarType()` | `Uber-PaymentService/.../repositories/FareConfigRepository.java` |
| Entity | `FareConfig` | — | `Uber-EntityService/.../Models/FareConfig.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - RuntimeException` | `"Fare config not found for car type: X"` | Insert fare config: `INSERT INTO fare_config (car_type, base_fare, per_km_rate, per_min_rate, surge_multiplier, created_at, updated_at) VALUES ('SEDAN', 50.00, 15.00, 2.00, 1.0, NOW(), NOW());` |
| `500 - IllegalArgumentException` | Invalid `carType` | Use: `XL`, `SEDAN`, `HATCHBACK`, `COMPACT_SUV`, `SUV` |

**Required Seed Data:**
```sql
INSERT INTO fare_config (car_type, base_fare, per_km_rate, per_min_rate, surge_multiplier, created_at, updated_at) VALUES
('SEDAN', 50.00, 15.00, 2.00, 1.0, NOW(), NOW()),
('HATCHBACK', 40.00, 12.00, 1.50, 1.0, NOW(), NOW()),
('SUV', 80.00, 20.00, 3.00, 1.0, NOW(), NOW()),
('XL', 100.00, 25.00, 4.00, 1.0, NOW(), NOW()),
('COMPACT_SUV', 60.00, 18.00, 2.50, 1.0, NOW(), NOW());
```

---

### 11.2 POST `/api/v1/payment/initiate`

**Purpose:** Initiate a payment for a booking (mock gateway).

**Request Body:**
```json
{
    "bookingId": 1,
    "amount": 180.00
}
```

**Success Response (200 OK):**
```json
{
    "orderId": "mock_order_1711100000000",
    "amount": 180.00,
    "currency": "INR",
    "status": "CREATED"
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `PaymentController` | `initiatePayment()` | `Uber-PaymentService/.../controllers/PaymentController.java` |
| Service | `PaymentGatewayService` | `initiatePayment()` | `Uber-PaymentService/.../services/PaymentGatewayService.java` |
| Repository | `PaymentRepository` | `save()` | `Uber-PaymentService/.../repositories/PaymentRepository.java` |
| Entity | `Payment` | — | `Uber-EntityService/.../Models/Payment.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - UnsupportedOperationException` | `payment.gateway.mock` is `false` | Set `payment.gateway.mock: true` in `application.yaml` |
| `500 - JPA error` | `payment` table missing | Verify `ddl-auto: update` or create table manually |

---

### 11.3 POST `/api/v1/payment/confirm`

**Purpose:** Confirm a payment (mock).

**Request Body:**
```json
{
    "paymentId": 1,
    "razorpayOrderId": "mock_order_123",
    "razorpayPaymentId": "mock_pay_456",
    "razorpaySignature": "mock_sig_789"
}
```

**Success Response (200 OK):**
```json
{
    "success": true,
    "paymentId": "mock_payment_1711100000000",
    "status": "COMPLETED",
    "amount": 0.0
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `PaymentController` | `confirmPayment()` | `Uber-PaymentService/.../controllers/PaymentController.java` |
| Service | `PaymentGatewayService` | `confirmPayment()` | `Uber-PaymentService/.../services/PaymentGatewayService.java` |

---

### 11.4 GET `/api/v1/wallet/balance`

**Purpose:** Get wallet balance for a user.

**Query Parameters:** `?userId=1&userType=PASSENGER`

**Valid `userType` values:** `PASSENGER`, `DRIVER`

**Success Response (200 OK):**
```json
{
    "balance": 500.00,
    "currency": "INR"
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `WalletController` | `getBalance()` | `Uber-PaymentService/.../controllers/WalletController.java` |
| Service | `WalletService` | `getBalance()`, `createWallet()` | `Uber-PaymentService/.../services/WalletService.java` |
| Repository | `WalletRepository` | `findByUserIdAndUserType()` | `Uber-PaymentService/.../repositories/WalletRepository.java` |
| Entity | `Wallet` | — | `Uber-EntityService/.../Models/Wallet.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - IllegalArgumentException` | Invalid `userType` | Use `PASSENGER` or `DRIVER` (uppercase) |
| Returns `0.00` balance | Wallet auto-created with zero balance | This is expected for new users |

---

### 11.5 POST `/api/v1/wallet/add`

**Purpose:** Add money to a user's wallet.

**Query Parameters:** `?userId=1&userType=PASSENGER`

**Request Body:**
```json
{
    "amount": 500.00
}
```

**Success Response (200 OK):** Empty body

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `WalletController` | `addMoney()` | `Uber-PaymentService/.../controllers/WalletController.java` |
| Service | `WalletService` | `addMoney()` | `Uber-PaymentService/.../services/WalletService.java` |
| Repository | `WalletRepository` | `findByUserIdAndUserType()`, `save()` | `Uber-PaymentService/.../repositories/WalletRepository.java` |

---

### 11.6 GET `/api/v1/billing/ride/{bookingId}`

**Purpose:** Get detailed ride invoice.

**Success Response (200 OK):**
```json
{
    "bookingId": 1,
    "baseFare": 50.00,
    "distanceFare": 100.00,
    "timeFare": 20.00,
    "surgeMultiplier": 1.20,
    "totalFare": 180.00,
    "commission": 18.00,
    "driverEarnings": 162.00,
    "paymentTime": "2026-03-22T10:45:00.000+00:00"
}
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BillingController` | `getRideInvoice()` | `Uber-PaymentService/.../controllers/BillingController.java` |
| Service | `BillingService` | `getRideInvoice()` | `Uber-PaymentService/.../services/BillingService.java` |
| Repository | `PaymentRepository` | `findByBookingId()` | `Uber-PaymentService/.../repositories/PaymentRepository.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - RuntimeException` | `"Payment not found for booking: X"` | Ensure payment record exists for this bookingId |

---

### 11.7 GET `/api/v1/billing/history`

**Purpose:** Get billing history for a user.

**Query Parameters:** `?userId=1&userType=PASSENGER`

**Success Response (200 OK):**
```json
[]
```

> **Note:** This endpoint currently returns an empty list (stub implementation).

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `BillingController` | `getBillingHistory()` | `Uber-PaymentService/.../controllers/BillingController.java` |
| Service | `BillingService` | `getBillingHistory()` | `Uber-PaymentService/.../services/BillingService.java` |

---

## 12. Notification Service APIs

**Base URL (Direct):** `http://localhost:8083`  
**Base URL (Gateway):** `http://localhost:8080/notification`

---

### 12.1 POST `/notifications/send`

**Purpose:** Send a notification (mock — logs to console).

**Request Body:**
```json
{
    "eventType": "DRIVER_ASSIGNED",
    "userId": 1,
    "userType": "PASSENGER",
    "payload": {
        "bookingId": 1,
        "driverId": 1
    }
}
```

**Valid `eventType` values:** `DRIVER_ASSIGNED`, `CAB_ARRIVED`, `RIDE_STARTED`, `RIDE_COMPLETED`, `RIDE_CANCELLED`

**Success Response (200 OK):**
```
"Notification sent successfully"
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `NotificationController` | `sendNotification()` | `Uber-NotificationService/.../controllers/NotificationController.java` |
| Service | `NotificationService` | `sendNotification()`, `sendPushNotification()`, `sendEmail()` | `Uber-NotificationService/.../services/NotificationService.java` |
| Kafka Consumer | `KafkaConsumerService` | `consumeNotificationEvent()` | `Uber-NotificationService/.../consumers/KafkaConsumerService.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500` | Unexpected `eventType` | Use valid event type from the list above |
| Kafka events not received | Consumer not subscribed | Check `notification-events-topic` exists in Kafka |

---

## 13. Socket/Kafka Service APIs

**Base URL (Direct):** `http://localhost:3002`  
**Base URL (Gateway):** `http://localhost:8080/socket`

---

### 13.1 POST `/api/socket/newride`

**Purpose:** Raise a ride request that gets broadcast to drivers via WebSocket/Kafka.

**Request Body:**
```json
{
    "bookingId": 1,
    "passengerId": 1,
    "pickupLat": 19.0760,
    "pickupLng": 72.8777,
    "dropLat": 19.1136,
    "dropLng": 72.8697,
    "driverIds": [1, 2, 3]
}
```

**Success Response (200 OK):**
```json
true
```

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverRequestController` | `raiseRideRequest()` | `Uber-SocketKafkaService/.../controllers/DriverRequestController.java` |
| Producer | `KafkaProducerService` | `sendRideRequest()` | `Uber-SocketKafkaService/.../producers/KafkaProducerService.java` |
| Consumer | `KafkaConsumerService` | `consumeRideRequest()` | `Uber-SocketKafkaService/.../consumers/KafkaConsumerService.java` |
| Consumer | `RideRequestTestConsumer` | — | `Uber-SocketKafkaService/.../consumers/RideRequestTestConsumer.java` |
| Config | `WebSocketConfig` | STOMP endpoint config | `Uber-SocketKafkaService/.../configurations/WebSocketConfig.java` |
| Config | `KafkaConfig` | Topic & serializer config | `Uber-SocketKafkaService/.../configurations/KafkaConfig.java` |

**Debugging — If API Fails:**

| Error | Cause | Fix |
|---|---|---|
| `500 - KafkaException` | Kafka broker unreachable | Verify Kafka on `localhost:9092` |
| `200` but drivers don't receive | WebSocket not connected | Drivers must connect via STOMP to `ws://localhost:3002/ws` |
| `200` but booking not updated | `rideResponseHandler()` not triggered | Driver must respond via WebSocket `/app/rideResponse/{userId}` |

### 13.2 WebSocket — Driver Ride Response

**STOMP Endpoint:** `ws://localhost:3002/ws`  
**Message Destination:** `/app/rideResponse/{userId}`

**Message Body:**
```json
{
    "response": true,
    "bookingId": 1,
    "driverId": 1
}
```

**When driver accepts (`response: true`):**
1. Sends `RideResponseDto` to Kafka
2. Calls `POST http://localhost:8001/api/v1/booking/{bookingId}` to assign driver
3. Sends notification event via Kafka

**Class & Function Flow:**
| Layer | Class | Method | File |
|---|---|---|---|
| Controller | `DriverRequestController` | `rideResponseHandler()` | `Uber-SocketKafkaService/.../controllers/DriverRequestController.java` |
| Producer | `KafkaProducerService` | `sendRideResponse()`, `sendNotificationEvent()` | `Uber-SocketKafkaService/.../producers/KafkaProducerService.java` |

---

## 14. Kafka Event Flows

### Topics and Producers/Consumers

| Topic | Producer | Consumer | Payload DTO |
|---|---|---|---|
| `ride-request-topic` | Socket Service | Socket Service (broadcast to WebSocket) | `RideRequestDto` |
| `ride-response-topic` | Socket Service | Booking Service | `RideResponseDto` |
| `booking-completed-topic` | Booking Service | Payment Service | `BookingCompletedEventDto` |
| `notification-events-topic` | Booking Service, Socket Service | Notification Service | `NotificationEventDto` |
| `payment-completed-topic` | Payment Service | (no consumer yet) | `PaymentCompletedEventDto` |

### Event Flow Diagram

```
Passenger creates booking
    │
    ▼
BookingService ──[Retrofit]──▶ LocationService (get nearby drivers)
    │
    ▼
BookingService ──[Retrofit]──▶ SocketService (POST /api/socket/newride)
    │
    ▼
SocketService ──[Kafka: ride-request-topic]──▶ SocketService Consumer
    │                                              │
    ▼                                              ▼
SocketService ──[WebSocket]──▶ Driver App     (broadcasts to drivers)
    │
    ▼ (driver responds via WebSocket)
SocketService ──[Kafka: ride-response-topic]──▶ BookingService Consumer
    │
    ├── SocketService ──[REST]──▶ BookingService (update booking with driver)
    │
    ├── SocketService ──[Kafka: notification-events-topic]──▶ NotificationService
    │
    ▼ (ride completes)
BookingService ──[Kafka: booking-completed-topic]──▶ PaymentService
    │
    ▼
PaymentService processes payment (deduct from passenger wallet, credit driver)
    │
    ▼
PaymentService ──[Kafka: payment-completed-topic]──▶ (no consumer yet)
```

### Required Kafka Topics

Create these topics if they don't exist:

```bash
kafka-topics.sh --create --topic ride-request-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic ride-response-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic booking-completed-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic notification-events-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic payment-completed-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

---

## 15. Inter-Service Communication Map

| Caller Service | Target Service | Protocol | Interface/Class |
|---|---|---|---|
| Booking Service | Location Service | Retrofit HTTP | `LocationServiceApi.java` → `POST /api/location/nearby/drivers` |
| Booking Service | Socket Service | Retrofit HTTP | `UberSocketApi.java` → `POST /api/socket/newride` |
| Driver Service | Location Service | Retrofit HTTP | `LocationServiceApi.java` → `POST /api/location/drivers` |
| Driver Service | Booking Service | Retrofit HTTP | `BookingServiceApi.java` → `GET /api/v1/booking/driver/{driverId}` |
| Socket Service | Booking Service | RestTemplate HTTP | `POST http://localhost:8001/api/v1/booking/{bookingId}` |
| Booking Service | Notification Service | Kafka | `notification-events-topic` |
| Booking Service | Payment Service | Kafka | `booking-completed-topic` |
| Socket Service | Booking Service | Kafka | `ride-response-topic` |
| Socket Service | Notification Service | Kafka | `notification-events-topic` |

---

## 16. Database Schema Reference

### Entity Relationship Summary

```
Passenger ──< Booking >── Driver
                │
         ExactLocation (start)
         ExactLocation (end)

Driver ──── Car ──── Color
Driver ──── ExactLocation (lastKnown, home)

Booking ──── Review ──── PassengerReview

Payment (bookingId references Booking.id)
Wallet (userId + userType)
FareConfig (carType)
OTP (sentToNumber)
```

### Key Tables

| Table | Entity | Key Columns |
|---|---|---|
| `passenger` | `Passenger` | id, firstName, lastName, email, phoneNumber, password, address, rating |
| `driver` | `Driver` | id, firstName, lastName, email, password, licenseNumber, aadharNumber, driverApprovalStatus, isAvailable, activeCity |
| `car` | `Car` | id, plateNumber, brand, model, carType, color_id, driver_id |
| `color` | `Color` | id, name |
| `booking` | `Booking` | id, bookingStatus, bookingDate, startTime, endTime, totalDistance, passenger_id, driver_id, startLocation_id, endLocation_id |
| `exact_location` | `ExactLocation` | id, latitude, longitude |
| `payment` | `Payment` | id, bookingId, amount, status, gatewayOrderId, gatewayPaymentId |
| `wallet` | `Wallet` | id, userId, userType, balance, currency |
| `fare_config` | `FareConfig` | id, carType, baseFare, perKmRate, perMinRate, surgeMultiplier |
| `booking_review` | `Review` | id, content, rating, booking_id |
| `passenger_review` | `PassengerReview` | id, passengerReviewContent, passengerRating |
| `otp` | `OTP` | id, code, sentToNumber |
| `dbconstant` | `DBConstant` | id, name, value |
| `named_location` | `NamedLocation` | id, name, zipCode, city, country, state, exactLocation_id |

---

## 17. End-to-End Test Scenarios

### Scenario 1: Complete Ride Flow

```
Step 1: Register passenger
    POST /api/v1/auth/signup/passenger

Step 2: Register driver
    POST /api/v1/driver/auth/signup

Step 3: Approve driver (SQL)
    UPDATE driver SET driver_approval_status = 'APPROVED' WHERE id = 1;

Step 4: Driver sign in
    POST /api/v1/driver/auth/signin

Step 5: Save driver location
    POST /api/location/drivers
    {"driverId": "1", "latitude": 19.076, "longitude": 72.877}

Step 6: Set driver available
    PUT /api/v1/driver/availability?lat=19.076&lng=72.877
    {"available": true}

Step 7: Create booking
    POST /api/v1/booking
    {"passengerId": 1, "startLocation": {"latitude": 19.076, "longitude": 72.877}, "endLocation": {"latitude": 19.113, "longitude": 72.869}}

Step 8: Check booking status
    GET /api/v1/booking/1

Step 9: (Driver accepts via WebSocket or manual update)
    POST /api/v1/booking/1
    {"status": "SCHEDULED", "driverId": 1}

Step 10: Update to CAB_ARRIVED
    PUT /api/v1/booking/1/status?status=CAB_ARRIVED

Step 11: Start ride
    PUT /api/v1/booking/1/status?status=IN_RIDE

Step 12: Complete ride
    PUT /api/v1/booking/1/status?status=COMPLETED

Step 13: Check ride invoice
    GET /api/v1/billing/ride/1

Step 14: Check wallet balance
    GET /api/v1/wallet/balance?userId=1&userType=PASSENGER
```

### Scenario 2: Fare Estimation

```
Step 1: Estimate fare
    POST /api/v1/fare/estimate
    {"startLat": 19.076, "startLng": 72.877, "endLat": 19.113, "endLng": 72.869, "carType": "SEDAN"}

Step 2: Compare with different car types
    POST /api/v1/fare/estimate (with carType: "SUV")
    POST /api/v1/fare/estimate (with carType: "HATCHBACK")
```

### Scenario 3: Wallet Operations

```
Step 1: Check initial balance
    GET /api/v1/wallet/balance?userId=1&userType=PASSENGER

Step 2: Add money
    POST /api/v1/wallet/add?userId=1&userType=PASSENGER
    {"amount": 1000.00}

Step 3: Verify new balance
    GET /api/v1/wallet/balance?userId=1&userType=PASSENGER
```

---

## 18. Common Errors & Troubleshooting

### Infrastructure Issues

| Symptom | Likely Cause | Resolution |
|---|---|---|
| `Connection refused` on any port | Service not started | Start the service, check startup order |
| `503 Service Unavailable` via Gateway | Service not registered in Eureka | Check Eureka dashboard at `http://localhost:8761` |
| `504 Gateway Timeout` | Target service is slow/hanging | Check target service logs, increase timeout |
| `429 Too Many Requests` | Rate limiter triggered | Wait or increase `replenishRate`/`burstCapacity` |

### Database Issues

| Symptom | Likely Cause | Resolution |
|---|---|---|
| `Table 'X' doesn't exist` | Schema not created | Set `ddl-auto: update` or run DDL manually |
| `DataIntegrityViolationException` | Unique constraint violated | Check for duplicate records |
| `Unable to acquire JDBC Connection` | MySQL not running or wrong credentials | Verify MySQL connection: `mysql -u root -proot` |
| Case-sensitivity in DB name | Linux is case-sensitive for DB names | Use consistent casing: `uber_db_local` |

### Kafka Issues

| Symptom | Likely Cause | Resolution |
|---|---|---|
| `KafkaException: Failed to construct kafka producer` | Kafka broker unreachable | Start Kafka + Zookeeper |
| Events published but not consumed | Topic doesn't exist or wrong group ID | Create topics manually, verify `groupId` |
| `SerializationException` | DTO class mismatch between producer/consumer | Ensure DTOs are identical across services |

### JWT/Auth Issues

| Symptom | Likely Cause | Resolution |
|---|---|---|
| `401 Unauthorized` everywhere | JWT secret mismatch between services | Verify all services use the same `jwt.secret` |
| Token works on direct URL but not gateway | Gateway uses `Authorization` header, service uses cookie | Use `Authorization: Bearer <token>` header for Gateway |
| `SignatureException` | Secret key changed after token was issued | Re-authenticate to get new token |

### Redis Issues

| Symptom | Likely Cause | Resolution |
|---|---|---|
| `RedisConnectionFailureException` | Redis not running | Start Redis: `redis-server` |
| No nearby drivers found | No GEO data in Redis | Save driver locations via API first |
| Config properties commented out | `application.yaml` has Redis config commented | Uncomment Redis properties |

### Retrofit/Inter-Service Issues

| Symptom | Likely Cause | Resolution |
|---|---|---|
| `ConnectException` in Retrofit calls | Target service offline | Start the target service |
| `404` from Retrofit call | URL path mismatch | Verify `RetrofitConfig.java` base URL |
| Async callback never fires | Retrofit `enqueue()` thread issue | Check logs for callback errors |

---

## Appendix A: Quick Reference — All Endpoints

| # | Method | Path | Service | Port |
|---|---|---|---|---|
| 1 | POST | `/api/v1/auth/signup/passenger` | Auth | 7475 |
| 2 | POST | `/api/v1/auth/signin/passenger` | Auth | 7475 |
| 3 | GET | `/api/v1/auth/validate` | Auth | 7475 |
| 4 | POST | `/api/v1/driver/auth/signup` | Driver | 8081 |
| 5 | POST | `/api/v1/driver/auth/signin` | Driver | 8081 |
| 6 | GET | `/api/v1/driver/auth/validate` | Driver | 8081 |
| 7 | GET | `/api/v1/driver/profile` | Driver | 8081 |
| 8 | PUT | `/api/v1/driver/availability` | Driver | 8081 |
| 9 | POST | `/api/v1/driver/location` | Driver | 8081 |
| 10 | POST | `/api/v1/booking` | Booking | 8001 |
| 11 | GET | `/api/v1/booking/{bookingId}` | Booking | 8001 |
| 12 | POST | `/api/v1/booking/{bookingId}` | Booking | 8001 |
| 13 | GET | `/api/v1/booking/passenger/{passengerId}` | Booking | 8001 |
| 14 | GET | `/api/v1/booking/driver/{driverId}` | Booking | 8001 |
| 15 | PUT | `/api/v1/booking/{bookingId}/status` | Booking | 8001 |
| 16 | POST | `/api/v1/booking/{bookingId}/cancel` | Booking | 8001 |
| 17 | POST | `/api/location/drivers` | Location | 7777 |
| 18 | POST | `/api/location/nearby/drivers` | Location | 7777 |
| 19 | POST | `/api/v1/fare/estimate` | Payment | 8082 |
| 20 | POST | `/api/v1/payment/initiate` | Payment | 8082 |
| 21 | POST | `/api/v1/payment/confirm` | Payment | 8082 |
| 22 | GET | `/api/v1/wallet/balance` | Payment | 8082 |
| 23 | POST | `/api/v1/wallet/add` | Payment | 8082 |
| 24 | GET | `/api/v1/billing/ride/{bookingId}` | Payment | 8082 |
| 25 | GET | `/api/v1/billing/history` | Payment | 8082 |
| 26 | POST | `/notifications/send` | Notification | 8083 |
| 27 | POST | `/api/socket/newride` | Socket | 3002 |
| 28 | WS | `/app/rideResponse/{userId}` | Socket | 3002 |

---

## Appendix B: Postman Collection

Import the file `UrbanLift_Postman_Collection.json` from this repository root into Postman to get all 27 REST API requests pre-configured and organized by service folder.

---

*End of Document*
