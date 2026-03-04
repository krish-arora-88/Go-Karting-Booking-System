# Codebase Map

> Updated: 2026-03-04. Legacy Java Swing files and flat-file JSON data are gone. This reflects the current production-grade stack.

---

## Frontend — Next.js 14 App Router

### Pages
| File | Route | Purpose |
|------|-------|---------|
| [app/page.tsx](../app/page.tsx) | `/` | Landing page: inline auth (Sign In / New Player tabs), perspective grid floor, Tron branding |
| [app/dashboard/page.tsx](../app/dashboard/page.tsx) | `/dashboard` | Race control: Tron sidebar, 7-day date strip, slots grid, BookingModal, BOOKINGS tab |
| [app/layout.tsx](../app/layout.tsx) | (root) | Loads Orbitron + DM Mono via `next/font/google`; wraps in `<Providers>` |
| [app/globals.css](../app/globals.css) | — | `@tailwind` directives + CSS custom props (`--bg`, `--cyan`, `--pink`, etc.) + utility classes (`.tron-input`, `.grid-floor-container`, `.bracket-corner`, etc.) |

### Components
| File | Purpose |
|------|---------|
| [components/ui/Button.tsx](../components/ui/Button.tsx) | Variants: `cyan` (default), `pink-ghost`, `dim-ghost`, `outline` |
| [components/booking/TimeSlotCard.tsx](../components/booking/TimeSlotCard.tsx) | Race entry pass: dashed border, corner brackets, Orbitron time display, capacity bar, `isPending` prop |
| [components/booking/BookingModal.tsx](../components/booking/BookingModal.tsx) | Fixed-overlay group booking modal: racer count stepper, dynamic name inputs, `.tron-input` fields |
| [components/Providers.tsx](../components/Providers.tsx) | Wraps app in TanStack QueryClientProvider + Toaster |

### State & Services
| File | Purpose |
|------|---------|
| [store/authStore.ts](../store/authStore.ts) | Zustand store: `token`, `username`, `login`, `logout`, `hasHydrated` (prevents flash redirect on refresh) |
| [services/authService.ts](../services/authService.ts) | `login()`, `register()`, `logout()`, `refreshToken()` — calls auth API endpoints |
| [services/bookingService.ts](../services/bookingService.ts) | `getTimeSlots(date)`, `createBooking(...)`, `getUserBookings()`, `cancelBooking(id)` — attaches Bearer token |

### Config
| File | Purpose |
|------|---------|
| [postcss.config.js](../postcss.config.js) | Enables Tailwind CSS processing (critical — without this no utility classes are generated) |
| [tailwind.config.js](../tailwind.config.js) | Custom colors (CSS var–based), `font-orbitron` / `font-mono` aliases, `tailwindcss-animate` plugin |
| [next.config.js](../next.config.js) | Next.js config |
| [tsconfig.json](../tsconfig.json) | TypeScript strict mode; `@/` path alias |
| [package.json](../package.json) | Next.js 14, React 18, TanStack Query v5, Zustand, Tailwind 3.3.6, tailwindcss-animate |

---

## Backend — Spring Boot 3.2.3 (Hexagonal Architecture)

### Root path: `backend/src/main/java/com/gokarting/`

### Domain (pure Java — no Spring, no JPA)
| File | Purpose |
|------|---------|
| `domain/model/Booking.java` | Booking domain model |
| `domain/model/TimeSlot.java` | Time slot domain model |
| `domain/model/User.java` | User domain model |
| `domain/model/OutboxEvent.java` | Outbox event domain model |
| `domain/event/BookingEvent.java` | Domain event: booking created/cancelled |
| `domain/event/SlotAvailabilityChangedEvent.java` | Domain event: slot capacity changed |
| `domain/port/out/BookingMetricsPort.java` | Metrics port interface (implemented by `infrastructure/metrics/BookingMetrics`) |
| `domain/exception/SlotFullException.java` | Thrown when slot at capacity |
| `domain/exception/DuplicateBookingException.java` | Thrown on duplicate active booking |
| `domain/exception/ResourceNotFoundException.java` | Thrown when entity not found |
| `domain/exception/UserAlreadyExistsException.java` | Thrown on duplicate username |

### Application (use cases / orchestration)
| File | Purpose |
|------|---------|
| `application/BookingService.java` | Create/cancel bookings; drains slot capacity; writes outbox event in same transaction |
| `application/AuthApplicationService.java` | Register/login/logout/refresh; JWT issuance and blacklisting |
| `application/TimeSlotService.java` | Query time slots with capacity for a given date |

### Inbound Adapters (`adapter/in/web/`)
| File | Endpoints |
|------|-----------|
| `AuthController.java` | POST `/api/auth/register`, `/login`, `/refresh`, `/logout` |
| `BookingController.java` | POST `/api/bookings`, GET `/api/bookings`, DELETE `/api/bookings/{id}` |
| `TimeSlotController.java` | GET `/api/timeslots?date=YYYY-MM-DD` |
| `GlobalExceptionHandler.java` | `@RestControllerAdvice` → RFC 7807 ProblemDetail; handles SlotFull, Duplicate, NotFound, BadCredentials, DataIntegrity, Validation, generic 500 |

### Outbound Adapters (`adapter/out/`)
| File | Purpose |
|------|---------|
| `persistence/entity/BookingEntity.java` | JPA entity for `bookings` table |
| `persistence/entity/TimeSlotEntity.java` | JPA entity; `@Version` for optimistic locking |
| `persistence/entity/UserEntity.java` | JPA entity for `users` table |
| `persistence/entity/OutboxEventEntity.java` | JPA entity for `outbox_events` table |
| `persistence/BookingJpaRepository.java` | Spring Data JPA repository |
| `persistence/TimeSlotJpaRepository.java` | Spring Data JPA repository |
| `persistence/UserJpaRepository.java` | Spring Data JPA repository |
| `persistence/OutboxEventJpaRepository.java` | Spring Data JPA repository |
| `persistence/BookingPersistenceAdapter.java` | Maps domain ↔ JPA entity |
| `persistence/OutboxPersistenceAdapter.java` | Saves `OutboxEvent` (domain) as `OutboxEventEntity` (JPA) |
| `kafka/KafkaOutboxPublisher.java` | Scheduler that polls outbox table and publishes to Kafka |

### Infrastructure
| File | Purpose |
|------|---------|
| `infrastructure/security/JwtService.java` | Issues + validates JWTs (JJWT 0.12.5); checks Redis blacklist |
| `infrastructure/security/SecurityConfig.java` | Spring Security filter chain; CORS; BCrypt bean |
| `infrastructure/security/RateLimitFilter.java` | Bucket4j + Redis distributed rate limiting (5 req/15min on auth) |
| `infrastructure/metrics/BookingMetrics.java` | Implements `BookingMetricsPort`; Micrometer counters/gauges |

### Configuration
| File | Purpose |
|------|---------|
| `backend/src/main/resources/application.yml` | DB URL, Redis, Kafka, JWT secret, Flyway config |
| `backend/src/main/resources/application-dev.yml` | Dev overrides |
| `backend/Dockerfile` | Multi-stage build (Maven build → JRE runtime) |
| `backend/pom.xml` | Spring Boot 3.2.3, Java 21, JJWT 0.12.5, Bucket4j 8.10.1, Lombok 1.18.38, Testcontainers, ArchUnit |

### Database Migrations (`backend/src/main/resources/db/migration/`)
| File | Description |
|------|-------------|
| `V1__init_schema.sql` | Create users, time_slots, bookings tables |
| `V2__seed_time_slots.sql` | Insert 24 time slots (12:00–23:59) |
| `V3__add_outbox_events.sql` | Create outbox_events table |
| `V4__add_booking_idempotency_key.sql` | Add `idempotency_key` column + unique index |
| `V5__add_racer_fields.sql` | Add `racer_count` int + `racer_names` JSONB to bookings |
| `V6__add_role_to_users.sql` | Add `role` column to users |
| `V7__add_refresh_tokens.sql` | Refresh token storage |
| `V8__add_booking_date.sql` | Add `booking_date` date column to bookings |
| `V9__fix_booking_unique_constraint.sql` | Replace full unique constraint with partial index `WHERE status = 'CONFIRMED'` |

### Tests (`backend/src/test/java/com/gokarting/`)
| File | Purpose |
|------|---------|
| `integration/BookingIntegrationTest.java` | Full booking lifecycle via Testcontainers (Postgres + Kafka + Redis) |
| `integration/AuthIntegrationTest.java` | Register/login/refresh/logout flows |
| `architecture/ArchitectureTest.java` | 4 ArchUnit rules: domain purity, layer isolation, no inbound→outbound calls |

---

## Infrastructure Files (project root)

| File | Purpose |
|------|---------|
| [docker-compose.yml](../docker-compose.yml) | Postgres, Redis, Kafka, Kafka-UI, Spring Boot app, Prometheus, Grafana |
| [.github/workflows/ci.yml](../.github/workflows/ci.yml) | CI: build backend (Maven), run tests (Testcontainers), build frontend (npm) |

---

## Inventory Summary

| Category | Count |
|----------|-------|
| Frontend TSX/TS files | ~12 |
| Backend Java files | ~35 |
| Flyway migration SQL files | 9 (V1–V9) |
| Integration + architecture tests | 3 test classes |
| Config files (root + backend) | ~10 |
| **Implemented API endpoints** | **8 of 8** ✅ |
