# Architecture Overview

> Updated: 2026-03-04. Reflects the fully-transformed distributed system deployed to Fly.io + Vercel.

---

## Production Deployment

```
┌─────────────────────────────────────────────────────────────────┐
│                   Vercel (Free Tier)                             │
│                                                                 │
│   Next.js 14 Frontend — apex-racing-gokarting.vercel.app        │
│   Static + Edge Functions · NEXT_PUBLIC_API_URL baked at build  │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTPS / REST (JWT Bearer)
┌───────────────────────────▼─────────────────────────────────────┐
│               Fly.io (Free / Pay-as-you-go)                     │
│                                                                 │
│   Spring Boot 3.2.3 — apex-racing-api.fly.dev                   │
│   shared-cpu-1x · 512MB · auto_stop_machines = "suspend"        │
│   Profile: no-kafka (Kafka/OutboxPoller disabled)                │
│                                                                 │
│   ┌─────────────────┐   ┌──────────────────────────────┐       │
│   │ Fly Postgres 17  │   │ Upstash Redis (pay-as-you-go)│       │
│   │ apex-racing-db   │   │ fly-apex-racing-redis         │       │
│   │ .flycast:5432    │   │ .upstash.io:6379              │       │
│   │ sslmode=disable  │   │ password-protected            │       │
│   │ Flyway V1–V9     │   │ JWT blacklist + Bucket4j      │       │
│   └─────────────────┘   └──────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Local Development

```
docker compose up              # Postgres, Redis, Kafka, Kafka-UI, Prometheus, Grafana, Spring Boot
npm run dev                    # Next.js → http://localhost:3000

docker-compose.prod.yml        # Prod-lite: Postgres + Redis + app only (no Kafka)
                               # Activates SPRING_PROFILES_ACTIVE=no-kafka
```

---

## System Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                    Next.js 14 Frontend (Port 3000)               │
│  app/page.tsx (inline auth)  →  app/dashboard/page.tsx           │
│  Zustand (authStore)  ·  TanStack Query v5  ·  Tailwind CSS      │
│  services/authService.ts  ·  services/bookingService.ts          │
└─────────────────────────┬────────────────────────────────────────┘
                          │ HTTP/REST  (JWT Bearer)
┌─────────────────────────▼────────────────────────────────────────┐
│              Spring Boot 3.2.3 Backend (Port 8080)               │
│                  Hexagonal (Ports & Adapters)                     │
│                                                                   │
│  adapter/in/web/          adapter/out/persistence/               │
│  ├── AuthController       ├── BookingRepository (JPA)            │
│  ├── BookingController    ├── TimeSlotRepository (JPA)           │
│  └── GlobalExceptionHandler └── OutboxPersistenceAdapter         │
│                                                                   │
│  domain/                  infrastructure/                        │
│  ├── model/               ├── config/KafkaConfig @Profile(!no-kafka)│
│  ├── port/out/            ├── outbox/OutboxPoller @Profile(!no-kafka)│
│  └── exception/           ├── metrics/BookingMetrics             │
│                           └── security/ (JWT + Redis)            │
└──────┬──────────────┬────────────────────┬───────────────────────┘
       │              │                    │ (dev only)
  ┌────▼────┐   ┌─────▼─────┐   ┌─────────▼────────┐
  │Postgres │   │  Redis 7  │   │   Apache Kafka    │
  │   16/17 │   │ /Upstash  │   │                   │
  │ Flyway  │   │ JWT black-│   │ booking-events    │
  │ V1–V9   │   │ list      │   │ topic (Outbox)    │
  │ JPA +   │   │ Rate lim- │   │ Dead Letter Queue │
  │ Optimis-│   │ iting     │   │ (disabled in prod)│
  │ tic lock│   │ (Bucket4j)│   └───────────────────┘
  └─────────┘   └───────────┘
       │
  ┌────▼───────────────────┐
  │  Prometheus + Grafana  │
  │  (dev only: 9090/3001) │
  └────────────────────────┘
```

---

## Frontend

### Design System — Tron/Synthwave Arcade
- **Palette**: `--bg #060614`, `--surface #0d0d2b`, `--cyan #00CFFF`, `--pink #FF2D6B`, `--green #7FFF00`, `--dim #4a4a7a`
- **Fonts**: Orbitron (display) via `font-orbitron` class · DM Mono (data) via `font-mono` class
- **Tailwind**: loaded via `postcss.config.js` → `tailwind.config.js` (content paths: `app/**`, `components/**`)

### State Management
| Layer | Tool | Responsibility |
|-------|------|----------------|
| Auth state | Zustand + `persist` | token, username, login/logout actions; `hasHydrated` flag gates redirects |
| Server state | TanStack Query v5 | time slots, user bookings; auto-invalidation on mutation; refetchInterval: 30s |
| Local UI | React `useState` | modal open/close, selected date, racer count |

### Auth Flow
1. Landing page (`app/page.tsx`) → inline Sign In / New Player tabs (no modal)
2. POST `/api/auth/login` → `{ token, refreshToken, username }`
3. Token stored in Zustand persisted store (localStorage); `hasHydrated` prevents flash redirect
4. Dashboard polls `hasHydrated` before checking auth; redirect to `/` if unauthenticated
5. Refresh token: POST `/api/auth/refresh` · Logout: POST `/api/auth/logout` (blacklists token in Redis)

### Booking Flow
1. Dashboard loads: `GET /api/timeslots?date=YYYY-MM-DD` → TanStack Query cache
2. Past slots filtered client-side (slots where `startTime` < now are hidden when today is selected)
3. User opens BookingModal → selects racer count + names → POST `/api/bookings`
4. On success: toast "SLOT BOOKED", invalidate slots + bookings queries, capacity updates
5. Cancel: `DELETE /api/bookings/{id}` → invalidate queries

---

## Backend

### Hexagonal Architecture Layers
```
adapter/in/web/          ← REST controllers (inbound)
application/             ← Use case services (orchestration)
domain/                  ← Pure Java; no Spring; no JPA
  model/                 ← Booking, TimeSlot, User, OutboxEvent
  port/out/              ← BookingMetricsPort (interface)
  exception/             ← SlotFullException, DuplicateBookingException, etc.
  event/                 ← BookingEvent, SlotAvailabilityChangedEvent
adapter/out/persistence/ ← JPA entities + repositories (outbound)
infrastructure/          ← metrics, security, config, outbox (not domain, not adapter)
```

ArchUnit enforces these boundaries (4 tests, all passing).

### Spring Profiles
| Profile | Behavior |
|---------|----------|
| (default) | Full stack: Kafka + OutboxPoller + all infra |
| `no-kafka` | Disables `KafkaAutoConfiguration`, skips `KafkaConfig` and `OutboxPoller` beans. Outbox events accumulate in DB but are not forwarded. Used in Fly.io production. |

### API Endpoints
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/auth/register` | No | Rate-limited (5/15min per IP) |
| POST | `/api/auth/login` | No | Rate-limited; intentionally vague error on bad creds |
| POST | `/api/auth/refresh` | No | Redis-backed refresh token rotation |
| POST | `/api/auth/logout` | Yes | Blacklists access token in Redis |
| GET | `/api/timeslots` | Yes | `?date=YYYY-MM-DD`; returns slots with capacity |
| POST | `/api/bookings` | Yes | Creates booking; `X-Idempotency-Key` header supported |
| GET | `/api/bookings` | Yes | User's own bookings |
| DELETE | `/api/bookings/{id}` | Yes | Cancel booking (sets status=CANCELLED) |

### Error Responses
All errors are RFC 7807 ProblemDetail: `type`, `title`, `status`, `detail`, `traceId`.
`GlobalExceptionHandler` maps domain exceptions → HTTP status codes.

### Concurrency & Correctness
- **Optimistic locking**: `@Version` on `TimeSlotEntity` prevents double-booking race conditions
- **Partial unique index** (`V9`): `uq_bookings_user_slot_date WHERE status = 'CONFIRMED'` — cancelled bookings don't block re-booking
- **Idempotency**: `uq_bookings_idempotency_key` prevents duplicate bookings on client retry
- **Group booking**: `racer_count` drains capacity (SUM, not COUNT); `racer_names` stored as JSONB array
- **Transactional Outbox**: Booking + OutboxEvent written in one DB transaction; Kafka publish is async via scheduler (dev only)

### Security
- JWT: 15-min access tokens + 7-day refresh tokens
- Redis blacklist: logout invalidates access token before expiry
- Rate limiting: Bucket4j + Redis distributed buckets (5 req / 15 min on auth endpoints)
- BCrypt password hashing
- Redis password support: `RedisConfig.lettuceProxyManager()` reads `spring.data.redis.password` for authenticated connections

---

## Infrastructure

### Local Dev (docker-compose.yml)
| Service | Port | Purpose |
|---------|------|---------|
| `postgres` | 5432 | Primary database; Flyway migrations V1–V9 |
| `redis` | 6379 | JWT blacklist + Bucket4j rate limiting |
| `kafka` | 9092 | Booking events (Transactional Outbox) |
| `kafka-ui` | 8081 | Kafka topic browser |
| `app` | 8080 | Spring Boot backend (multi-stage Dockerfile) |
| `prometheus` | 9090 | Metrics scraping |
| `grafana` | 3001 | Dashboards |

### Prod-Lite (docker-compose.prod.yml)
Postgres + Redis + app only. Requires `DB_PASSWORD` and `JWT_SECRET` env vars. Activates `no-kafka` profile.

Frontend runs separately: `npm run dev` → port 3000.

---

## Database Schema (key tables)

```sql
users           — id, username, password_hash, role, created_at
time_slots      — id, start_time, end_time, capacity, version (optimistic lock)
bookings        — id, user_id, time_slot_id, booking_date, status, racer_count,
                  racer_names (JSONB), idempotency_key, created_at
outbox_events   — id, aggregate_type, aggregate_id, event_type, payload (JSONB),
                  published, created_at
```

Flyway migration history: V1 (schema) → V9 (partial unique index on bookings).
