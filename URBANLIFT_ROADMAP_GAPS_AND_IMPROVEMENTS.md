# UrbanLift — Remaining Work, Improvements & Feature Roadmap

> **Purpose:** Honest assessment of what is incomplete, what feels “off” or incorrect in flows, concrete fixes, and ideas for new APIs/features.  
> **Audience:** Product owners, backend engineers, and architects.  
> **Last updated:** March 23, 2026

---

## 0. Critical roadmap items — resolution status (code verified)

The items in **§2 Critical issues** and **§8 Quick-win checklist** were re-checked against the repo. Below is what is **done in code** vs **still intentional backlog** (product/infra).

| # | Topic | Status |
|---|--------|--------|
| 2.1 | Gateway JWT whitelist vs real Auth/Driver paths | **Fixed** — `JwtAuthenticationFilter` allows `/auth/api/v1/auth/signup`, `/signin`, and `/driver/api/v1/driver/auth/signup`, `/signin`, `/validate` (plus legacy `/auth/login` `/register`). |
| 2.2 | Passenger JWT subject hardcoded | **Fixed earlier** — `createToken(claims, authRequestDto.getEmail())` with real email; **now also** `passengerId` + `role` claims. |
| 2.3 | Auth not on Eureka | **Fixed** — `Uber-AuthService` includes `spring-cloud-starter-netflix-eureka-client`; `application.name: Uber-AuthService`. |
| 2.4 | Booking Retrofit Eureka names | **Fixed** — `UBER-LOCATIONSERVICE` / `UBER-SOCKETKAFKASERVICE` match Spring Eureka’s uppercased service IDs. |
| 2.5 | Redis `Point` lat/lon order | **Fixed** — `new Point(longitude, latitude)` for add/radius; readback maps `latitude = point.getY()`, `longitude = point.getX()`. |
| 2.6 | Kafka ride-response ignored | **Fixed** — `KafkaConsumerService` calls `bookingService.updateBooking` idempotently. |
| 2.7 | Payment confirm mock no DB | **Fixed** — `confirmPayment` loads `Payment` by id, sets `COMPLETED`, saves. |
| 2.8 | Billing invoice hardcoded lines | **Improved** — line items are a **display split** that sums to `totalFare` until components are persisted (see `BillingService` comment). |
| 2.9 | Socket notification passenger id | **Fixed** — `fetchPassengerIdForBooking` + Booking `/passenger-id` API. |
| 2.10 | Booking complete fare hardcoded | **Improved** — `estimateFareForCompletedRide` uses distance-based formula (not flat `100.0`). |
| 2.11 | `X-User-Id` was email | **Fixed** — Gateway sets **`X-User-Id`** from `passengerId`/`driverId` claims; **`X-User-Email`** from JWT subject; JWTs issued on sign-in include ids. |
| 2.12 | Inter-service HTTP without auth | **Open** — needs mTLS / internal API keys / OAuth2 client credentials (not a one-line fix). |
| §3 | Real payment gateways, FCM, billing history, E2E tests, etc. | **Open** — roadmap / product backlog (see §3, §6). |
| §8 | Empty `RideService` | **Removed** from codebase (no `RideService.java`). |

**Note:** §4–§7 (design notes, observability, OpenAPI, features) remain **recommendations**, not bugs.

---

## Table of contents

0. [Critical roadmap items — resolution status](#0-critical-roadmap-items--resolution-status-code-verified)  
1. [Executive summary](#1-executive-summary)
2. [Critical issues & incorrect flows](#2-critical-issues--incorrect-flows)
3. [What is clearly “remaining” (incomplete)](#3-what-is-clearly-remaining-incomplete)
4. [Things that feel off (design / consistency)](#4-things-that-feel-off-design--consistency)
5. [Recommended improvements (non-feature)](#5-recommended-improvements-non-feature)
6. [Feature & API ideas](#6-feature--api-ideas)
7. [Suggested prioritization](#7-suggested-prioritization)
8. [Quick-win checklist](#8-quick-win-checklist)

---

## 1. Executive summary

UrbanLift already has a **credible microservices skeleton**: auth, driver, booking, location, payment, notifications, real-time socket/Kafka, API Gateway, and Eureka. **Most critical gaps in §2 are now addressed in code** (see **§0 Resolution status**): Gateway public routes, JWT claims + headers, Eureka for Auth, Retrofit service IDs, Redis GEO point order, Kafka ride-response, payment confirm persistence, passenger id for notifications, and distance-based completed-ride fare. Remaining gaps are mainly **§2.12 service-to-service auth**, **real payment providers**, **observability/OpenAPI**, and **product features** in §3/§6.

**Next highest impact (after §0 fixes):**

1. **Service-to-service authentication** (mTLS, internal API keys, or OAuth2 client credentials) — §2.12.  
2. **Persist fare breakdown** on booking/payment so invoices are not derived splits — §2.8 follow-up.  
3. **Real payment gateway** + webhooks when `payment.gateway.mock=false`.  
4. **Integration / contract tests** (Testcontainers, WireMock) — §5.5.

---

## 2. Critical issues & incorrect flows

### 2.1 API Gateway JWT filter vs Auth Service reality

**What the Gateway does (`JwtAuthenticationFilter`):**

- Requires `Authorization: Bearer <token>` for almost every path.
- Treats only these as **open** (no JWT): paths starting with `/auth/login` or `/auth/register`.

**What Auth Service actually exposes:**

- `POST /api/v1/auth/signup/passenger`
- `POST /api/v1/auth/signin/passenger`
- `GET  /api/v1/auth/validate`

**Through the Gateway** (with rewrite `/auth/**` → `/**`), real paths look like:

- `POST http://localhost:8080/auth/api/v1/auth/signup/passenger`
- `POST http://localhost:8080/auth/api/v1/auth/signin/passenger`

None of these match `/auth/login` or `/auth/register`, so **signup/signin are still treated as “secured”** by the Gateway filter → clients get **401** unless they send a Bearer token they do not yet have.

**Why it feels broken:** The Gateway and Auth Service were designed under **different assumptions** (REST paths + cookie auth vs header JWT + wrong whitelist paths).

**Fix direction:**

- Whitelist the real public routes, e.g. prefixes:
  - `/auth/api/v1/auth/signup/`
  - `/auth/api/v1/auth/signin/`
- Or normalize Auth to `/api/v1/auth/login` and `/api/v1/auth/register` and whitelist those after rewrite.
- Optionally support **cookie-based JWT** at the Gateway (WebFlux cookie reader) if mobile/web clients rely on `JWT_TOKEN` / `DRIVER_JWT`.

---

### 2.2 Passenger sign-in issues JWT with wrong subject

In `AuthController.signin()`, the code calls:

```java
String jwtToken = jwtService.createToken("Abc@gmail");
```

So **every** successful login gets a token whose **subject is a hardcoded email**, not `authRequestDto.getEmail()`.

**Effects:**

- Validation against the real user is wrong unless the user happens to match that subject.
- Gateway sets `X-User-Id` from `claims.getSubject()` → downstream services may think **every user is the same identity** (email `Abc@gmail`), not a numeric user id.

**Fix:** `jwtService.createToken(authRequestDto.getEmail())` (or better: embed `userId`, `role`, `type` as claims).

---

### 2.3 Auth Service is not on Eureka; Gateway uses `lb://Uber-AuthService`

`Uber-AuthService` **has no Eureka client** in its `build.gradle`, while `GatewayConfig` routes auth to:

```text
.uri("lb://Uber-AuthService")
```

**Effect:** Unless you added Eureka elsewhere, **load-balanced resolution for Auth through the Gateway will fail** (503 / unknown host).

**Fix options:**

- Add **spring-cloud-starter-netflix-eureka-client** to Auth and register as `Uber-AuthService`, **or**
- Change Gateway auth route to a **direct URL** (e.g. `http://localhost:7475`) for local/dev, **or**
- Use **Spring Cloud LoadBalancer** with static service list.

---

### 2.4 Booking Service Retrofit → Eureka application names likely wrong

`Uber-BookingService` `RetrofitConfig` resolves:

| Variable in code | Likely registered Eureka name (`spring.application.name`) |
|------------------|-------------------------------------------------------------|
| `LOCATIONSERVICE` | `Uber-LocationService` |
| `UBERSOCKETSERVER` | `Uber-SocketKafkaService` |

**Driver Service** uses `UBER-LOCATIONSERVICE` and `UBER-BOOKINGSERVICE` (closer to normal Eureka uppercase VIP style, but still must match registration).

Netflix `getNextServerFromEureka(appName, ...)` is **sensitive to the exact app name** registered. If Booking uses a **non-existent** name, **nearby driver lookup and ride broadcast silently fail** in async Retrofit callbacks (errors only in logs).

**Fix:** Use the **exact** `spring.application.name` values (or verified Eureka `vipAddress`) consistently across all Retrofit configs; add **integration tests** or startup health checks that fail fast if lookup fails.

---

### 2.5 Redis GEO: `Point` latitude / longitude order

In `RedisLocationServiceImpl.saveDriverLocation()`:

```java
new Point(latitude, longitude)
```

Spring Data Redis documents **`Point` as x = longitude, y = latitude** for geographic use. Passing `(latitude, longitude)` **swaps** them vs Redis GEO conventions.

**Effect:** Stored positions and **radius queries can be wrong** (wrong hemisphere / nonsense distances), so “nearby drivers” may be empty or incorrect in real deployments.

**Fix:** Use `new Point(longitude, latitude)` **or** the API your Spring Data Redis version documents explicitly; add a unit test with known coordinates.

---

### 2.6 Ride-response Kafka path does not update booking

`Uber-BookingService` `KafkaConsumerService.consumeRideResponse()` contains:

```java
// TODO: handle booking confirmation logic here
```

Meanwhile, **Socket** `DriverRequestController` **does** call Booking via `RestTemplate` when a driver accepts — so **some** paths update the booking, but **Kafka-based** ride responses are **ignored** by Booking.

**Why it feels inconsistent:** Two channels (HTTP vs Kafka) for the same business event; one is wired, the other is not.

**Fix:** Either:

- Implement the consumer to **idempotently** assign driver / update status from `RideResponseDto`, **or**
- Remove the topic if HTTP is the single source of truth, and document that clearly.

---

### 2.7 Payment confirm does not update database (mock path)

`PaymentGatewayService.confirmPayment()` in mock mode builds a success DTO but **does not** locate the `Payment` row created at initiate and **does not** set status to `COMPLETED` / store gateway IDs.

**Effect:** Reporting, refunds, and reconciliation are impossible; **billing/invoice** may not align with reality.

**Fix:** Find payment by `gatewayOrderId` or `paymentId`, update status, emit events after commit.

---

### 2.8 Billing invoice uses hardcoded line items

`BillingService.getRideInvoice()` takes **real** `totalFare` from `Payment` but sets **fixed** `baseFare`, `distanceFare`, `timeFare`, `surgeMultiplier` (e.g. 50, 100, 20, 1.2).

**Effect:** Invoice **does not match** fare engine output or ride reality.

**Fix:** Persist fare breakdown on booking completion or store on `Payment` / `RideInvoice` entity.

---

### 2.9 Socket notification uses hardcoded passenger id

`DriverRequestController` sets:

```java
notificationEvent.setUserId(1L); // TODO: Get passenger ID from booking
```

**Effect:** Wrong user gets notification events in real scenarios.

**Fix:** Load booking by `bookingId` (Booking API or read model) and set `passengerId`.

---

### 2.10 Booking completion → payment uses hardcoded fare

`BookingServiceImpl` on `COMPLETED`:

```java
event.setFare(BigDecimal.valueOf(100.0)); // TODO: get from booking
```

**Effect:** Wallet debits/credits and downstream payment logic **ignore actual fare**.

**Fix:** Compute fare at ride end (distance/time/surge) or call Payment’s fare service; persist on `Booking`.

---

### 2.11 `X-User-Id` is email, not id

Gateway sets:

```java
.header("X-User-Id", claims.getSubject())
```

If subject is email, the header name **misleading**; services that expect numeric IDs will break.

**Fix:** Put `sub` = stable user id, add `email` claim; forward `X-User-Id` as numeric id and `X-User-Email` separately.

---

### 2.12 Inter-service HTTP without auth

Booking → Location/Socket and Socket → Booking use **Retrofit/RestTemplate without** forwarding end-user JWT or service-to-service credentials.

**Effect:** Any service on the network can hit Booking/Location if exposed; no **zero-trust** between services.

**Fix:** mTLS, internal API keys, or OAuth2 client credentials for service calls.

---

## 3. What is clearly “remaining” (incomplete)

| Area | Evidence | Notes |
|------|----------|--------|
| **Booking ride response (Kafka)** | TODO in `KafkaConsumerService` | Decide single channel; implement or remove |
| **Real payment gateways** | `UnsupportedOperationException` when `mock: false` | Razorpay/Stripe SDK, webhooks, idempotency |
| **Payment confirm persistence** | Mock confirm returns success only | Tie to DB + webhook signature verify |
| **Billing history** | `BillingService.getBillingHistory()` returns `List.of()` | Needs query by user + pagination |
| **Notifications** | `NotificationService` logs only | FCM, APNS, SMS, email providers |
| **Fare on booking complete** | Hardcoded `100.0` | Integrate `FareCalculationService` or stored quote |
| **Passenger ID on driver-assigned notification** | TODO in Socket controller | Fetch from booking |
| **Socket `RideService`** | Empty class | Either implement or delete |
| **Driver approval** | No admin API; only SQL | Back-office or admin service |
| **Auth on Gateway** | Wrong public paths + Auth not on Eureka | Blocks “single entry” testing |
| **Passenger JWT subject** | Hardcoded `"Abc@gmail"` | Must fix for any multi-user scenario |
| **E2E automated tests** | Not observed in scope | Contract + integration tests missing |

---

## 4. Things that feel off (design / consistency)

1. **Two auth stacks:** Passenger (Auth Service + cookie `JWT_TOKEN`) vs Driver (Driver Service + `DRIVER_JWT` + optional Bearer). Consider **one IAM service** with `role` / `userType` claims, or shared token format.  
2. **Duplicate DTOs** across Booking, Socket, Notification (`RideRequestDto`, `NotificationEventDto`, etc.) → **schema drift** risk when evolving Kafka payloads. Prefer **shared contract module** (e.g. `urbanlift-contracts`).  
3. **Database naming:** `Uber_DB_local` (Auth) vs `uber_db_local` (others) → deployment confusion on Linux.  
4. **Mixed async patterns:** Retrofit `enqueue` in Booking with **no** correlation id / tracing → hard to debug failed driver dispatch.  
5. **Rate limiting** at Gateway without Redis-backed limiter in config snippet → may not behave as expected in multi-instance Gateway (verify implementation).  
6. **Booking status** string in API vs enum internally → invalid strings cause `valueOf` exceptions (400 vs 500 handling not uniform).  
7. **WebSocket + REST** both in Socket service → document clearly which clients must use for accept/reject.  
8. **`driverId` in DTOs** sometimes `String`, sometimes `Long` → parsing edge cases in `BookingServiceImpl` (already has try/catch to null).

---

## 5. Recommended improvements (non-feature)

### 5.1 Observability

- **Structured logging** with `bookingId`, `passengerId`, `driverId`, `traceId`.  
- **OpenTelemetry** or Spring Cloud Sleuth for distributed traces across Gateway → services → Kafka.  
- **Micrometer** metrics: booking created, assign latency, Kafka lag, Redis errors.

### 5.2 API quality

- Global **`@ControllerAdvice`** with problem+json (`RFC 7807`).  
- **Validation** on DTOs (`@Valid`, `@NotNull`, coordinate ranges).  
- **OpenAPI 3** (Springdoc) per service + aggregated doc at Gateway.

### 5.3 Security

- **Secrets** out of YAML (env / Vault); rotate JWT secret.  
- **HTTPS** in production; `secure(true)` on cookies.  
- **CORS** policy explicit for web apps.  
- **RBAC** at Gateway: passenger vs driver routes (path-based or scope-based).

### 5.4 Data & resilience

- **Idempotency keys** on `POST /booking` and payment initiate.  
- **Outbox pattern** for Kafka publishes (avoid lost events if DB commit fails).  
- **Circuit breakers** (Resilience4j) on Retrofit/RestTemplate calls.  
- **Flyway/Liquibase** instead of `ddl-auto` in shared DB.

### 5.5 Testing

- **Testcontainers** for MySQL, Kafka, Redis in CI.  
- **WireMock** or MockWebServer for inter-service HTTP.  
- **Pact** or Spring Cloud Contract for API compatibility between services.

---

## 6. Feature & API ideas

Below are **new or expanded APIs** that would make the product closer to a real ride-hailing platform. Grouped by domain.

### 6.1 Passenger experience

| API / capability | Method & path (suggested) | Purpose |
|------------------|---------------------------|---------|
| Get active ride | `GET /api/v1/booking/passenger/{id}/active` | Single current trip |
| Fare quote before book | `POST /api/v1/booking/quote` | Uses Payment fare + surge rules |
| Cancel with reason | `POST /api/v1/booking/{id}/cancel` + body | Analytics + refunds policy |
| Rate driver after trip | `POST /api/v1/booking/{id}/review` | Stars + text → `Review` entities |
| Saved places | CRUD `/api/v1/passenger/places` | Home/work shortcuts |
| Push device registration | `POST /api/v1/passenger/devices` | FCM token storage |

### 6.2 Driver experience

| API / capability | Method & path (suggested) | Purpose |
|------------------|---------------------------|---------|
| Driver approval workflow | Admin `PUT /api/v1/admin/drivers/{id}/approval` | `APPROVED` / `DENIED` + reason |
| Earnings dashboard | `GET /api/v1/driver/earnings?from=&to=` | Aggregate from payments |
| Shift / session | `POST /api/v1/driver/shift/start` / `end` | Compliance, analytics |
| Document upload | `POST /api/v1/driver/documents` | License, insurance URLs |
| Reject ride | `POST /api/v1/booking/{id}/reject` (driver) | With reason, re-offer to next driver |

### 6.3 Matching & dispatch

| API / capability | Notes |
|------------------|--------|
| **Batch offer** with timeout | First-accept wins; auto-expire |
| **Surge pricing** by zone/time | Config service or rule engine |
| **ETA** | Google/OSRM integration |
| **Driver scoring** | Acceptance rate, cancellation rate |

### 6.4 Payments & billing

| API / capability | Notes |
|------------------|--------|
| **Payment methods** | Cards, UPI, wallet-only flags |
| **Refunds / partial refunds** | After cancel or dispute |
| **Invoices PDF** | `GET /api/v1/billing/ride/{id}/pdf` |
| **Tips** | Add to completed ride |
| **Promo codes** | `POST /api/v1/promo/apply` |

### 6.5 Safety & compliance

| API / capability | Notes |
|------------------|--------|
| **SOS / share trip** | `POST /api/v1/safety/sos` |
| **Trip sharing link** | Tokenized read-only status |
| **Audit log** | Admin-readable event stream |

### 6.6 Operations & admin

| API / capability | Notes |
|------------------|--------|
| **Feature flags** | Kill switch surge, new city |
| **Support tools** | Search user, force-cancel booking |
| **Analytics export** | BigQuery / warehouse |

### 6.7 Real-time

| Capability | Notes |
|------------|--------|
| **Unified WebSocket** namespace | `/topic/passenger/{id}`, `/topic/driver/{id}` |
| **Heartbeat** | Driver online stale detection |
| **Chat** | `ChatRequest` / `ChatResponse` DTOs exist but need full flow |

---

## 7. Suggested prioritization

### Phase A — “System works end-to-end” (1–2 sprints)

1. Fix Auth JWT subject + Gateway public paths (+ Auth Eureka or static URI).  
2. Fix Booking Retrofit Eureka names + verify Location/Socket calls in logs.  
3. Fix Redis `Point` order.  
4. Implement or remove Booking Kafka ride-response consumer.  
5. Payment confirm → DB update; fare from booking or calculator on complete.

### Phase B — “Production hygiene” (1–2 sprints)

- Flyway, centralized error format, OpenAPI, secrets management, basic integration tests.

### Phase C — “Product features”

- Reviews, admin approval, quotes, earnings, promos, SOS, etc., as product prioritizes.

---

## 8. Quick-win checklist

Use this as a sprint board of **small, high-value** tasks:

- [x] `AuthController`: JWT includes **`passengerId`** + **`role`**, subject = email (`createToken(claims, email)`).  
- [x] `JwtAuthenticationFilter`: whitelist **`/auth/api/v1/auth/signup|signin`** and **`/driver/api/v1/driver/auth/**`** public auth paths.  
- [x] Register **Auth** with Eureka (`spring-cloud-starter-netflix-eureka-client` + `Uber-AuthService`).  
- [x] `RetrofitConfig` (Booking): Eureka IDs **`UBER-LOCATIONSERVICE`** / **`UBER-SOCKETKAFKASERVICE`**.  
- [x] `RedisLocationServiceImpl`: **`Point(longitude, latitude)`** + correct readback to DTO.  
- [x] `KafkaConsumerService` (Booking): ride-response updates booking.  
- [x] `PaymentGatewayService.confirmPayment`: persists **`COMPLETED`** on `Payment`.  
- [x] `DriverRequestController`: passenger id from Booking API for notifications.  
- [x] `BookingServiceImpl`: fare from **`estimateFareForCompletedRide`** (distance-based).  
- [x] Gateway: **`X-User-Id`** from JWT claims (**`passengerId`/`driverId`**), **`X-User-Email`** from subject; Driver JWT includes **`driverId`**.  

---

## Related documents

- [`URBANLIFT_API_TESTING_GUIDE.md`](./URBANLIFT_API_TESTING_GUIDE.md) — API reference, debugging, and Postman collection pointer.  
- [`UrbanLift_Postman_Collection.json`](./UrbanLift_Postman_Collection.json) — Request collection.

---

*This document is based on static analysis of the repository as of the date above. Re-verify after refactors.*
