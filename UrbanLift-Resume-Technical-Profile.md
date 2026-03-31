# UrbanLift — Complete Technical Profile for Resume

This document is a complete inventory of the technologies, engineering patterns, and production-style practices used in UrbanLift. You can reuse it directly for your resume, LinkedIn, portfolio, and interview storytelling.

---

## Resume headline (short)

Built a distributed ride-hailing platform using Spring Boot microservices, Kafka event choreography, Redis geo + distributed locking, JWT cookie auth, and a React/TypeScript realtime frontend with durable Redux state.

---

## Project scope (what the system does)

- Multi-role product: passenger app, driver app, QA automation hub.
- Core workflows: signup/signin, fare estimation, booking, driver assignment, trip lifecycle, payment, ratings, notifications.
- Cross-service integration using REST + Kafka + WebSocket.
- Shared domain model with service-specific behavior and APIs.

---

## Services and responsibilities

| Service | Primary responsibility | Key methods/patterns |
|---|---|---|
| Auth Service | Passenger identity, JWT session validation | JWT in HttpOnly cookie, Spring Security filter chain |
| Driver Service | Driver onboarding/profile/availability/location | Driver status workflow, coordinate update APIs |
| Booking Service | Booking lifecycle orchestration | Idempotency key, Redlock-style concurrency guard, state machine transitions |
| Payment Service | Fare/payment flow + settlement side effects | Payment state transitions, wallet credit/debit logic, Kafka consumer settlement |
| Location Service | Nearby-driver lookup | Redis GEO indexing/query |
| Socket/Kafka Service | Realtime ride request + location fanout | STOMP/SockJS topic push, Kafka bridge |
| Notification Service | Event-driven notifications | Kafka consumer-driven side effects |
| Entity Service | Shared JPA entities/enums | Shared kernel pattern for domain model reuse |
| Frontend (`urbanlift-web`) | Passenger/driver UI + QA page | Typed API layer, Zod forms, Redux persist, realtime subscriptions |

---

## Architecture patterns used

- **Microservices decomposition**: business capabilities split into independently deployable services.
- **API Gateway + Service Discovery**: edge routing with Eureka-based dynamic lookup.
- **Event-driven architecture**: Kafka topics for asynchronous business events (booking completed, ride request/response, notifications).
- **Choreography-based flow**: service reactions to domain events rather than one central orchestrator for every step.
- **Shared kernel**: common entity module used by multiple services (fast iteration tradeoff vs coupling).
- **Backend-for-frontend style behavior**: frontend talks to multiple domain services with consistent client abstractions.

---

## Reliability, consistency, and correctness patterns

### 1) Idempotency
- **Pattern**: request deduplication using `Idempotency-Key`.
- **Applied in**: booking creation.
- **Value**: protects against duplicate rides when client retries due to timeout/network instability.

### 2) Distributed locking (Redlock-style)
- **Pattern**: Redis lock with lease/TTL and local fallback lock manager.
- **Applied in**: booking acquisition path to prevent concurrent active bookings per passenger.
- **Value**: prevents race conditions in high-concurrency windows.

### 3) Explicit state machine transitions
- **Pattern**: guarded transitions (`ASSIGNING_DRIVER -> SCHEDULED -> CAB_ARRIVED -> IN_RIDE -> COMPLETED/CANCELLED`).
- **Applied in**: booking status updates.
- **Value**: impossible-state prevention and predictable lifecycle.

### 4) Transactional boundaries
- **Pattern**: `@Transactional` on mutation workflows.
- **Applied in**: status changes, payment confirmation, ratings.
- **Value**: ACID consistency inside service boundaries.

### 5) Query-shaping for performance
- **Pattern**: fetch joins and targeted repository methods to reduce N+1.
- **Applied in**: booking detail/list queries, profile joins.
- **Value**: lower DB round trips and stable latency.

### 6) Numeric route constraints
- **Pattern**: strict path variable patterns (for example `{bookingId:\d+}`).
- **Applied in**: booking controller route definitions.
- **Value**: avoids route shadowing bugs and ambiguous endpoint matching.

---

## Data and persistence patterns

- **Relational persistence** with MySQL + JPA/Hibernate.
- **Schema migration discipline** with Flyway.
- **Repository abstraction** with domain-specific finder/update methods.
- **DTO boundary pattern** for API/Kafka payload contracts.
- **Validation** using `jakarta.validation` + typed schemas on frontend.

---

## Messaging and asynchronous integration

- Kafka producers/consumers for inter-service side effects.
- JSON payload contracts (`BookingCompletedEventDto`, notification DTOs, ride request/response DTOs).
- Deserializer hardening for cross-service DTO compatibility (type-header handling).
- Event-driven payment settlement trigger on booking completion.
- Notification fanout from lifecycle events.

---

## Security patterns and practices

- JWT session in **HttpOnly cookies** (`JWT_TOKEN`) to reduce XSS token theft risk.
- Spring Security filter-based authentication for validate endpoints.
- Role-separated passenger and driver auth journeys.
- Consistent 401/403 style handling through API error layers.
- CORS and environment-driven endpoint configuration for controlled cross-origin behavior.

---

## Realtime and geo patterns

- **WebSocket/STOMP pub-sub** for driver location streaming per booking topic.
- **Redis GEO spatial indexing** for nearby-driver retrieval.
- **Polling + push hybrid** in frontend (poll booking detail + subscribe to live location).
- **Demo stream presets** for deterministic location simulation in test/demo mode.

---

## Payment and wallet patterns

- Payment entity status lifecycle (`PENDING`, `COMPLETED`, etc.).
- Mock gateway abstraction for local/dev integration without external PSP dependency.
- Wallet ledger model for passenger/driver balances.
- Driver-side credit on completion event.
- Defensive null checks and wallet auto-create paths for safe wallet operations.
- Separation of **trip completion** event from **explicit rider checkout** UI flow.

---

## Frontend engineering patterns

- React 18 + TypeScript strict typing.
- React Hook Form + Zod runtime schema validation.
- Reusable API client wrapper (`apiFetch/apiJson`) with central error parsing.
- Redux Toolkit slice design for durable trip/payment context.
- `redux-persist` for refresh-safe state (active booking, payment draft, ride context).
- Component-driven UI architecture with typed DTO contracts.
- Vite proxy-based local integration across services.

---

## Quality and developer-experience practices

- Centralized exception handling in backend services.
- Structured logging in core business workflows.
- Build validation using Gradle (`compileJava`) and TypeScript/Vite build checks.
- API testing artifacts and service readmes for onboarding.
- QA page for automated end-to-end business flow checks.

---

## Resume-ready impact bullets (copy/paste)

- Designed and implemented a microservices ride-hailing platform with Spring Boot, Kafka, Redis, MySQL, and React/TypeScript across passenger, driver, booking, payment, location, and notification domains.
- Built idempotent booking creation using request keys and distributed lock protection (Redlock-style) to eliminate duplicate active rides under retry/concurrency scenarios.
- Implemented event-driven booking-to-payment flow via Kafka consumers/producers with hardened JSON deserialization for cross-service DTO compatibility.
- Developed realtime trip tracking using WebSocket/STOMP + Redis GEO-backed driver discovery and hybrid polling/push UI updates.
- Introduced Redux Toolkit + redux-persist in frontend to preserve booking and payment context across reloads, reducing user journey breakage.
- Enforced lifecycle correctness with explicit booking state transition guards and transactional service boundaries.

---

## FAANG-level discussion points (what interviewers usually ask)

1. How idempotency and locking interact under retries, duplicate HTTP requests, and partial failures.
2. How you would guarantee exactly-once settlement with Kafka redelivery (idempotent consumers, dedupe keys, outbox/inbox tables).
3. How you would evolve shared DTO/event schemas without breaking old consumers.
4. What SLOs and observability stack you would add first (tracing, metrics, dashboards, alerting).
5. How you would scale location + realtime fanout (partitioning strategy, topic design, sticky sessions, geo-sharding).
6. What security hardening is next (refresh-token strategy, CSRF defenses for cookies, secret rotation, audit logs).

---

## Technology stack checklist

**Backend:** Java, Spring Boot, Spring Data JPA, Spring Security, Spring Kafka, Retrofit, Netflix Eureka, Flyway, Lombok  
**Data:** MySQL, Redis (GEO + lock support)  
**Messaging:** Apache Kafka  
**Frontend:** TypeScript, React, Vite, Tailwind CSS, React Router, React Hook Form, Zod, Redux Toolkit, redux-persist, STOMP/SockJS  
**Build/Tooling:** Gradle, npm, Vite, environment-based config, service readmes and API guides  

---

## Honest maturity statement (good for interviews)

UrbanLift demonstrates strong production-aligned patterns (idempotency, distributed locking, event-driven boundaries, strict state transitions, typed contracts, and realtime UX). For true large-scale production, the next steps are outbox/inbox reliability, deeper observability, stronger security hardening, and higher automated test coverage with integration contract tests.

