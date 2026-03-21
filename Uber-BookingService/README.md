# Uber-BookingService

Core booking orchestration microservice. Creates bookings, fetches nearby drivers, raises ride requests via Kafka/WebSocket, and updates booking status when drivers accept.

---

## Overview

| Property | Value |
|----------|-------|
| **Port** | 8001 |
| **Database** | MySQL `uber_db_local` |
| **Eureka** | Registered |
| **Kafka** | Consumer: `ride-response-topic` |

---

## Architecture & Implementation

### Component Structure

```
Uber-BookingService/
├── controllers/
│   ├── BookingController.java       # REST endpoints
│   └── RetrofitConfig.java          # Retrofit + Eureka for HTTP clients
├── services/
│   ├── BookingService.java
│   └── BookingServiceImpl.java      # Core orchestration logic
├── repositories/
│   ├── BookingRepository.java
│   ├── PassengerRepository.java
│   └── DriverRepository.java
├── apis/                            # Retrofit interfaces
│   ├── LocationServiceApi.java      # → LocationService
│   └── UberSocketApi.java           # → SocketKafkaService
├── consumers/
│   └── KafkaConsumerService.java    # ride-response-topic
├── configurations/
│   └── KafkaConfig.java
└── dtos/
    ├── CreateBookingDto, CreateBookingResponseDto
    ├── UpdateBookingRequestDto, UpdateBookingResponseDto
    ├── RideRequestDto, RideResponseDto
    ├── NearbyDriversRequestDto, DriverLocationDto
```

### Create Booking Flow

1. **POST /api/v1/booking** receives `CreateBookingDto`.
2. Validate passenger exists; create `Booking` with status `ASSIGNING_DRIVER`.
3. Build `NearbyDriversRequestDto` from `startLocation` (lat/lng).
4. **Async:** Call `LocationServiceApi.getNearbyDrivers()` (Retrofit).
5. On success: call `UberSocketApi.raiseRideRequest()` with `RideRequestDto` (passengerId, bookingId).
6. Return `CreateBookingResponseDto` (bookingId, bookingStatus) immediately.

### Update Booking Flow

1. **POST /api/v1/booking/{id}** receives `UpdateBookingRequestDto` (status, driverId).
2. Load driver; call `bookingRepository.updateBookingStatusAndDriverById()`.
3. Return `UpdateBookingResponseDto`.

### Kafka Consumer

- **Topic:** `ride-response-topic`
- **Group:** `booking-group`
- **Payload:** `RideResponseDto` (response, bookingId)
- **Current behavior:** Logs only; TODO: handle booking confirmation (e.g. call updateBooking with driverId)

---

## Outgoing API Calls

| Target | Method | Endpoint | Purpose |
|-------|--------|----------|---------|
| **LocationService** | POST | `/api/location/nearby/drivers` | Get nearby drivers by lat/lng |
| **SocketKafkaService** | POST | `/api/socket/newride` | Raise ride request (→ Kafka → WebSocket) |

**Discovery:** Retrofit uses Eureka with service names `LOCATIONSERVICE` and `UBERSOCKETSERVER`. Eureka app names are `Uber-LocationService` and `Uber-SocketKafkaService` — ensure these match or update RetrofitConfig.

---

## APIs

| Method | Endpoint | Request Body | Response |
|-------|----------|--------------|----------|
| POST | `/api/v1/booking` | `CreateBookingDto` | `CreateBookingResponseDto` (201) |
| POST | `/api/v1/booking/{bookingId}` | `UpdateBookingRequestDto` | `UpdateBookingResponseDto` (200) |

### DTOs

**CreateBookingDto:** `passengerId`, `startLocation` (lat, lng), `endLocation` (lat, lng)

**UpdateBookingRequestDto:** `status` (e.g. SCHEDULED), `driverId` (Optional)

---

## Database

- **Tables:** `booking`, `passenger`, `driver` (via EntityService)
- **Custom query:** `updateBookingStatusAndDriverById` for status + driver update

---

## Known Issues & Gaps

1. **NearbyDriversRequestDto bug:** Uses `getEndLocation().getLongitude()` instead of `getStartLocation().getLongitude()` for nearby search.
2. **RideRequestDto incomplete:** Sent with only `passengerId` and `bookingId`; SocketKafkaService `RideRequestDto` supports `pickupLat`, `pickupLng`, `dropLat`, `dropLng`, `driverIds` — not populated.
3. **Kafka consumer TODO:** `ride-response-topic` consumer does not call `updateBooking`; driver acceptance is handled by SocketKafkaService calling BookingService directly.
4. **Eureka service names:** `LOCATIONSERVICE` / `UBERSOCKETSERVER` may not match registered app names.
5. **Booking entity:** `bookingDate`, `startTime`, `endTime` are required but not set in `createBooking`.
6. **UberSocketApi path:** `@POST("api/socket/newride")` — ensure base URL from Eureka includes or omits trailing slash correctly.

---

## Production Readiness Checklist

| Area | Recommendation |
|------|----------------|
| **Fix bugs** | Correct NearbyDriversRequestDto; set bookingDate/startTime/endTime; pass full RideRequestDto |
| **Eureka** | Align Retrofit service names with Eureka app names |
| **Resilience** | Add Retry, Circuit Breaker (Resilience4j) for LocationService and SocketKafkaService calls |
| **Idempotency** | Idempotency keys for create/update booking |
| **Validation** | Validate passengerId, locations, driverId before DB operations |
| **Auth** | Validate JWT from AuthService before creating/updating bookings |
| **Observability** | Distributed tracing (Sleuth/Zipkin), structured logging, metrics |
| **Kafka** | Handle consumer failures; dead-letter queue for failed ride responses |
| **Tests** | Unit tests for BookingServiceImpl; integration tests with Testcontainers (MySQL, Kafka) |
