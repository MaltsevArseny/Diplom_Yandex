# ExploreWithMe — Microservices Architecture

## Overview

ExploreWithMe is an event discovery platform split into independent microservices. All client requests are routed through a single API Gateway (port 8080). Services discover each other via Eureka and communicate via OpenFeign with Resilience4j circuit-breaker fallbacks.

---

## Architecture

```
Client
  │
  ▼
gateway-server (8080)       ← Spring Cloud Gateway
  ├── /admin/users/**       → user-service
  ├── /admin/events/**      → event-service
  ├── /admin/categories/**  → event-service
  ├── /admin/compilations/**→ event-service
  ├── /events/**            → event-service
  ├── /categories/**        → event-service
  ├── /compilations/**      → event-service
  ├── /users/*/events/**    → event-service
  ├── /users/*/requests/**  → request-service
  └── /hit, /stats          → stats-server

Infrastructure:
  discovery-server (8761)  ← Eureka Server
  config-server (8888)     ← Spring Cloud Config (native)
```

### Microservices

| Service | Description | Database | Tables |
|---|---|---|---|
| **event-service** | Events, categories, compilations | `ewm-event` | events, categories, compilations, compilation_events |
| **user-service** | User management | `ewm-user` | users |
| **request-service** | Participation requests | `ewm-request` | requests |
| **stats-server** | Hit statistics | `ewm-stat` | endpoint_hits |

### Inter-service Communication

All internal calls use **OpenFeign** with **Resilience4j** circuit-breaker fallbacks:

- `event-service` → `user-service` (`/internal/users/**`) — fetch initiator info for event DTOs
- `event-service` → `request-service` (`/internal/requests/**`) — get/update request statuses
- `request-service` → `event-service` (`/internal/events/**`) — validate event state, update confirmed count

**Fallback behavior (when a downstream service is unavailable):**
- User info unavailable → events returned with empty initiator name
- Request service unavailable → request lists return empty, status update returns empty result
- Event service unavailable → request creation returns 503

---

## Internal API

### user-service internal endpoints
| Method | Path | Description |
|---|---|---|
| GET | `/internal/users/{id}` | Get UserShortDto by id |
| GET | `/internal/users?ids=1,2,3` | Batch get UserShortDto list |
| GET | `/internal/users/{id}/exists` | Check if user exists |

### event-service internal endpoints
| Method | Path | Description |
|---|---|---|
| GET | `/internal/events/{eventId}` | Get event info needed by request-service |
| PATCH | `/internal/events/{eventId}/confirmed-requests?delta=1` | Increment/decrement confirmed requests count |

### request-service internal endpoints
| Method | Path | Description |
|---|---|---|
| GET | `/internal/requests/events?eventId=X` | Get requests for an event |
| POST | `/internal/requests/status-update` | Bulk update request statuses |
| GET | `/internal/requests/confirmed-count?eventIds=1,2,3` | Get confirmed counts map |

---

## External API

See the [OpenAPI specification](https://raw.githubusercontent.com/yandex-praktikum/java-plus-graduation/main/ewm-main-service-spec.json) for the full external API.

---

## Configuration

All service configurations are managed by **config-server** (Spring Cloud Config, native profile):

| Config file | Service |
|---|---|
| `infra/config-server/src/main/resources/config/event-service.yml` | event-service |
| `infra/config-server/src/main/resources/config/user-service.yml` | user-service |
| `infra/config-server/src/main/resources/config/request-service.yml` | request-service |
| `infra/config-server/src/main/resources/config/stats-server.yml` | stats-server |

---

## Running with Docker Compose

```bash
docker compose up --build
```

Services start in order: discovery-server → config-server → databases → core services → gateway.

All Postman tests should be sent to **http://localhost:8080** (the gateway).
