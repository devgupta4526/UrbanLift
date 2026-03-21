# Uber-ServiceDiscovery-Eureka

Eureka server for service discovery. Microservices register themselves and resolve each other by name instead of hardcoded URLs.

---

## Overview

| Property | Value |
|----------|-------|
| **Port** | 8761 |
| **Dashboard** | http://localhost:8761 |
| **Mode** | Standalone (does not register with another Eureka) |

---

## Architecture & Implementation

### Component Structure

```
Uber-ServiceDiscovery-Eureka/
└── UberServiceDiscoveryEurekaApplication.java   # @EnableEurekaServer
```

### Configuration

```yaml
eureka:
  instance:
    hostname: my-registry
  client:
    register-with-eureka: false   # This server doesn't register itself
    fetch-registry: false         # No peer replication
```

---

## Registered Services

When running, these services register with Eureka (by `spring.application.name`):

| Service | App Name | Port |
|---------|----------|------|
| Uber-BookingService | Uber-BookingService | 8001 |
| Uber-LocationService | Uber-LocationService | 7777 |
| Uber-SocketKafkaService | Uber-SocketKafkaService | 3002 |

**AuthService** and **EntityService** do not register.

---

## How Other Services Use Eureka

- **BookingService:** RetrofitConfig uses `EurekaClient.getNextServerFromEureka(serviceName, false)` for `LOCATIONSERVICE` and `UBERSOCKETSERVER`. **Service names must match** what Eureka has (typically `Uber-LocationService`, `Uber-SocketKafkaService`).
- **SocketKafkaService:** Uses hardcoded `http://localhost:8001` for BookingService instead of Eureka — should be updated to use discovery.

---

## Outgoing / Incoming API Calls

**None.** Eureka is a registry; clients call it for instance lookup, but Eureka itself does not call application APIs.

---

## Running

```bash
cd UrbanLift/Uber-ServiceDiscovery-Eureka/Uber-ServiceDiscovery-Eureka
./gradlew bootRun
```

Start Eureka **before** BookingService, LocationService, and SocketKafkaService so they can register on startup.

---

## Production Readiness Checklist

| Area | Recommendation |
|------|----------------|
| **HA** | Run multiple Eureka nodes with `register-with-eureka: true`, `fetch-registry: true` for peer replication |
| **Security** | Add HTTP basic auth or OAuth for Eureka dashboard and REST API |
| **Service names** | Document and enforce naming (e.g. `Uber-LocationService`) for all clients |
| **Health** | Monitor Eureka for availability |
