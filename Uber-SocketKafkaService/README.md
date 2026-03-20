# Uber-SocketKafkaService

Real-time ride request and driver response service. Accepts ride requests via HTTP, publishes to Kafka, broadcasts to drivers via WebSocket, and forwards driver acceptances to BookingService.

---

## Overview

| Property | Value |
|----------|-------|
| **Port** | 3002 |
| **Database** | MySQL `uber_db_local` (JPA present; minimal usage) |
| **Eureka** | Registered |
| **Kafka** | Producer: `ride-request-topic`, `ride-response-topic`; Consumer: `ride-request-topic` |
| **WebSocket** | STOMP over SockJS at `/ws` |

---

## Architecture & Implementation

### Component Structure

```
Uber-SocketKafkaService/
├── controllers/
│   └── DriverRequestController.java   # HTTP + STOMP message handler
├── producers/
│   └── KafkaProducerService.java
├── consumers/
│   └── KafkaConsumerService.java     # ride-request-topic → WebSocket
├── configurations/
│   ├── KafkaConfig.java
│   ├── WebSocketConfig.java
│   └── ScheduleConfig.java
├── services/
│   └── RideService.java              # (minimal/empty)
└── dtos/
    ├── RideRequestDto.java
    ├── RideResponseDto.java
    ├── UpdateBookingRequestDto.java
    └── ChatRequest.java, ChatResponse.java
```

### Flow

#### Ride Request (HTTP → Kafka → WebSocket)

1. **POST /api/socket/newride** receives `RideRequestDto`.
2. `KafkaProducerService.sendRideRequest(dto)` → produce to `ride-request-topic`.
3. Return `200 OK` with `true`.

4. **KafkaConsumerService** consumes from `ride-request-topic`.
5. `messagingTemplate.convertAndSend("/topic/rideRequest", dto)` → broadcast to all WebSocket subscribers.

#### Driver Response (WebSocket → Kafka + HTTP)

1. Driver sends STOMP message to `/app/rideResponse/{userId}` with `RideResponseDto` (response, bookingId).
2. `DriverRequestController.rideResponseHandler()`:
   - Produce to `ride-response-topic`.
   - Call `POST http://localhost:8001/api/v1/booking/{bookingId}` with `RideResponseDto` as body.

### WebSocket Config

- **Endpoint:** `/ws` (SockJS)
- **Broker:** `/topic`, `/queue`
- **App prefix:** `/app`
- **Subscription for drivers:** `/topic/rideRequest`
- **Driver response:** `/app/rideResponse/{userId}` (userId = driver/session identifier)

### Kafka Topics

| Topic | Role | Payload |
|-------|------|---------|
| `ride-request-topic` | Producer (HTTP), Consumer (WebSocket broadcast) | `RideRequestDto` |
| `ride-response-topic` | Producer (driver response) | `RideResponseDto` |

---

## APIs

### REST

| Method | Endpoint | Request Body | Response |
|-------|----------|--------------|----------|
| POST | `/api/socket/newride` | `RideRequestDto` | `Boolean` (200) |

### WebSocket (STOMP)

| Destination | Direction | Payload |
|-------------|-----------|---------|
| `/topic/rideRequest` | Server → Client | `RideRequestDto` |
| `/app/rideResponse/{userId}` | Client → Server | `RideResponseDto` |

### DTOs

**RideRequestDto:** `bookingId`, `passengerId`, `pickupLat`, `pickupLng`, `dropLat`, `dropLng`, `driverIds` (optional)

**RideResponseDto:** `response` (Boolean), `bookingId` — **missing `driverId`** for BookingService update

---

## Outgoing API Calls

| Target | Method | Endpoint | Purpose |
|-------|--------|----------|---------|
| **BookingService** | POST | `http://localhost:8001/api/v1/booking/{bookingId}` | Update booking when driver accepts |

**Issue:** URL is hardcoded. BookingService expects `UpdateBookingRequestDto` (status, driverId), but `RideResponseDto` has no `driverId` — request will fail or not update driver correctly.

---

## Incoming API Calls (Who Calls This)

| Caller | Endpoint | Purpose |
|--------|----------|---------|
| **BookingService** | `/api/socket/newride` | Raise ride request after getting nearby drivers |

---

## Known Issues & Gaps

1. **Hardcoded BookingService URL:** Use Eureka or config property instead of `http://localhost:8001`.
2. **RideResponseDto missing driverId:** BookingService `updateBooking` needs `driverId`; add to `RideResponseDto` and map in controller.
3. **userId in path:** `/app/rideResponse/{userId}` — clarify if userId is driverId; ensure it is passed to BookingService.
4. **Broadcast to all:** `/topic/rideRequest` broadcasts to all subscribers; consider targeting only nearby drivers (e.g. via `driverIds` in `RideRequestDto`).
5. **Chat DTOs:** `ChatRequest`, `ChatResponse` exist but no chat implementation visible.

---

## Production Readiness Checklist

| Area | Recommendation |
|------|----------------|
| **Service discovery** | Use Eureka/RestTemplate or Feign for BookingService URL |
| **RideResponseDto** | Add `driverId`; map to `UpdateBookingRequestDto` before calling BookingService |
| **Targeted broadcast** | Use `/queue/rideRequest/{driverId}` for driver-specific delivery when `driverIds` is set |
| **Error handling** | Retry/circuit breaker for BookingService call; dead-letter for failed Kafka produce |
| **Auth** | Validate JWT for HTTP `/newride`; validate driver identity for WebSocket |
| **Observability** | Tracing, metrics for Kafka and WebSocket |
| **Tests** | Integration tests with embedded Kafka and WebSocket client |
