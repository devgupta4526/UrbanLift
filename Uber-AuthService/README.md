# Uber-AuthService

Authentication and authorization microservice for UrbanLift. Handles passenger signup, signin, and JWT-based session validation.

---

## Overview

| Property | Value |
|----------|-------|
| **Port** | 7475 |
| **Database** | MySQL `Uber_DB_local` |
| **Eureka** | Not registered |
| **Dependencies** | Uber-EntityService (shared models) |

---

## Architecture & Implementation

### Component Structure

```
Uber-AuthService/
├── controllers/
│   └── AuthController.java          # REST endpoints
├── services/
│   ├── AuthService.java             # Signup business logic
│   ├── JwtService.java              # JWT creation & validation
│   └── UserDetailsServiceImpl.java  # Spring Security UserDetails
├── repositories/
│   └── PassengerRepository.java     # JPA (from EntityService)
├── filters/
│   └── JwtAuthFilter.java           # JWT validation for /validate
├── configurations/
│   └── SpringSecurity.java          # Security config
└── dto/
    ├── AuthRequestDto.java
    ├── AuthResponseDto.java
    ├── PassengerDto.java
    └── PassengerSignUpRequestDto.java
```

### Flow

1. **Signup** → `AuthService.signUpPassenger()` → BCrypt hash → Save `Passenger` → Return `PassengerDto`
2. **Signin** → `AuthenticationManager.authenticate()` → JWT created → Set `JWT_TOKEN` HttpOnly cookie → Return success
3. **Validate** → Read `JWT_TOKEN` cookie → `JwtService` validates → Return success/fail

### Security Configuration

- **PermitAll:** `/api/v1/auth/signup`, `/api/v1/auth/signin`
- **Authenticated:** `/api/v1/auth/validate`
- **JwtAuthFilter:** Runs only for `/api/v1/auth/validate` (extracts JWT from cookie, sets `SecurityContext`)

### JWT Details

- **Algorithm:** HMAC-SHA (via JJWT)
- **Storage:** HttpOnly cookie `JWT_TOKEN`, path `/`, 7-day max age
- **Config:** `jwt.expiry` (seconds), `jwt.secret` in `application.yaml`

---

## APIs

| Method | Endpoint | Request Body | Response |
|-------|----------|--------------|----------|
| POST | `/api/v1/auth/signup/passenger` | `PassengerSignUpRequestDto` | `PassengerDto` (201) |
| POST | `/api/v1/auth/signin/passenger` | `AuthRequestDto` (email, password) | `AuthResponseDto` (200) + cookie |
| GET | `/api/v1/auth/validate` | — (cookie required) | `AuthResponseDto` or error (401) |

### DTOs

**PassengerSignUpRequestDto:** `firstName`, `lastName`, `email`, `phoneNumber`, `password`, `address`

**AuthRequestDto:** `email`, `password`

---

## Outgoing API Calls

**None.** AuthService is standalone; no calls to other microservices.

---

## Database

- **Table:** `passenger` (via EntityService `Passenger` entity)
- **JPA:** `ddl-auto: validate` (schema managed by Flyway in EntityService)

---

## Known Issues & Gaps

1. **Security matcher mismatch:** `SpringSecurity` permits `/signup` and `/signin`, but controller uses `/signup/passenger` and `/signin/passenger` — signup/signin may be blocked.
2. **JwtAuthFilter logic:** `if (token != null) return 401` appears inverted; should reject when token is *absent*.
3. **Hardcoded JWT subject:** Signin uses `jwtService.createToken("Abc@gmail")` instead of `authRequestDto.getEmail()`.
4. **No driver auth:** Only passenger signup/signin; no driver registration or login.

---

## Production Readiness Checklist

| Area | Recommendation |
|------|----------------|
| **Security** | Fix JwtAuthFilter logic; use actual user email in JWT; align security matchers with controller paths |
| **Driver Auth** | Add driver signup/signin and role-based access |
| **Refresh Tokens** | Implement refresh token flow for long-lived sessions |
| **Rate Limiting** | Add rate limiting on signin to prevent brute force |
| **Input Validation** | Validate email format, password strength, phone format |
| **Audit** | Log signup/signin events for security auditing |
| **Eureka** | Register with Eureka if other services need to discover AuthService |
| **Secrets** | Move JWT secret to vault/env; never commit to repo |
| **CORS** | Configure CORS for frontend origins |
| **Health** | Add `/actuator/health` for Kubernetes/load balancer probes |
| **Tests** | Unit + integration tests for AuthService, JwtService, filters |

---

## Running

```bash
cd UrbanLift/Uber-AuthService/Uber-AuthService
./gradlew bootRun
```

Ensure MySQL is running with `Uber_DB_local` created and schema applied (via EntityService Flyway).
