# Uber-EntityService

Shared entity library and database schema for UrbanLift. Provides JPA entities, enums, and Flyway migrations. **Not a runnable microservice** — it is a dependency published to Maven Local and consumed by AuthService, BookingService, and SocketKafkaService.

---

## Overview

| Property | Value |
|----------|-------|
| **Port** | 7476 (when run as app; typically used as library) |
| **Database** | MySQL `Uber_Db_Local` |
| **Schema** | Flyway migrations |
| **Publish** | `com.example:Uber-EntityService:0.0.1-SNAPSHOT` |

---

## Architecture & Implementation

### Component Structure

```
Uber-EntityService/
├── Models/
│   ├── BaseModel.java           # id, createdAt, updatedAt
│   ├── Passenger.java
│   ├── Driver.java
│   ├── Booking.java
│   ├── Car.java
│   ├── ExactLocation.java
│   ├── NamedLocation.java
│   ├── OTP.java
│   ├── Review.java
│   ├── PassengerReview.java
│   ├── Color.java
│   ├── CarType.java
│   ├── DriverApprovalStatus.java
│   └── BookingStatus.java
└── resources/db/migration/
    ├── V1__init_db.sql
    ├── V2__add_Car.sql
    ├── V3__add_dbconstants.sql
    ├── V4__add_location_and_otp.sql
    ├── V5__add_reviews.sql
    └── V6__add_more_details_to_passenger.sql
```

### Entity Relationships

| Entity | Key Fields | Relations |
|--------|------------|-----------|
| **Passenger** | firstName, lastName, email, phoneNumber, password, address, rating | bookings, activeBooking, lastKnownLocation, home |
| **Driver** | firstName, lastName, email, licenseNumber, aadharNumber, driverApprovalStatus, activeCity, isAvailable, rating | car, lastKnownLocation, home, bookings |
| **Booking** | bookingStatus, bookingDate, startTime, endTime, totalDistance | passenger, driver, startLocation, endLocation |
| **Car** | plateNumber, brand, model, carType | color, driver |
| **ExactLocation** | latitude, longitude | — |
| **NamedLocation** | name, zipCode, city, state, country | exactLocation |

### Enums

- **BookingStatus:** SCHEDULED, CANCELLED, CAB_ARRIVED, ASSIGNING_DRIVER, IN_RIDE, COMPLETED
- **DriverApprovalStatus:** APPROVED, DENIED, PENDING
- **CarType:** XL, SUV, COMPACT_SUV, SEDAN, HATCHBACK

### Database Tables (Flyway)

| Table | Purpose |
|-------|---------|
| `passenger` | Passenger accounts |
| `driver` | Driver accounts, license, aadhar, approval, availability |
| `booking` | Bookings with status, passenger, driver, locations |
| `car` | Cars linked to drivers |
| `color` | Car colors |
| `exact_location` | Lat/lng coordinates |
| `named_location` | Addresses with city, state, country |
| `otp` | OTP codes for verification |
| `booking_review` | Reviews for bookings |
| `passenger_review` | Passenger-specific review data |
| `dbconstant` | Key-value constants |

---

## Outgoing / Incoming API Calls

**None.** EntityService is a library; it does not make or receive HTTP calls.

---

## Consumers (Who Uses This)

| Service | Usage |
|---------|-------|
| **AuthService** | Passenger entity, PassengerRepository |
| **BookingService** | Booking, Passenger, Driver, ExactLocation, BookingStatus |
| **SocketKafkaService** | DTOs, models (minimal) |

---

## Publishing

```gradle
publishing {
    repositories {
        maven { url = uri('E:/Development/Uber/...') }  // Local path
    }
}
```

**Note:** Publish path is machine-specific. For team use, publish to a shared Maven repo (Nexus, Artifactory) or ensure `mavenLocal()` is used and EntityService is built/published before dependent services.

---

## Build & Publish

```bash
cd UrbanLift/Uber-EntityService/Uber-EntityService
./gradlew publishToMavenLocal
```

Other services reference: `implementation 'com.example:Uber-EntityService:0.0.1-SNAPSHOT'`

---

## Known Issues & Gaps

1. **Database name inconsistency:** V1 uses `Uber_DB_local` / `uber_db_local` / `Uber_Db_Local` across services — standardize.
2. **Booking required fields:** `bookingDate`, `startTime`, `endTime` are NOT NULL but BookingServiceImpl does not set them.
3. **Flyway scope:** EntityService runs Flyway; consuming services use `ddl-auto: validate`. Ensure EntityService migrations run first (or a dedicated schema service runs them).

---

## Production Readiness Checklist

| Area | Recommendation |
|------|----------------|
| **DB naming** | Unify database name across all services |
| **Versioning** | Semantic versioning for library; consumers pin versions |
| **Migrations** | Run Flyway from a single service or CI pipeline |
| **Audit** | Consider soft deletes, audit columns in BaseModel |
| **Indexes** | Add indexes for frequent queries (e.g. booking_status, passenger_id, driver_id) |
