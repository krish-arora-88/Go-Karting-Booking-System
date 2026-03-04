# Design Issues & Technical Debt

> Updated: 2026-03-04. All legacy issues (missing booking API, plaintext passwords, God object, file-based persistence, missing PostCSS config, wrong unique constraint) are **resolved**. This file tracks the current open items only.

---

## Resolved (for reference)

| Issue | Fix Applied |
|-------|------------|
| Booking API endpoints missing | Full hexagonal backend implemented (BookingController + services + JPA) |
| Plaintext password comparison (Swing) | Legacy Swing removed; BCrypt used throughout |
| God object `MainMenu.java` | Legacy removed; clean hexagonal layers |
| File-based persistence (race conditions) | PostgreSQL + JPA + optimistic locking (`@Version`) |
| Missing `postcss.config.js` | Created at project root — Tailwind utilities now generated |
| `uq_bookings_user_slot_date` blocking re-book after cancel | V9 migration: partial unique index `WHERE status = 'CONFIRMED'` |
| `DataIntegrityViolationException` returning 500 | `GlobalExceptionHandler` now catches it → 409 CONFLICT with ProblemDetail |
| No auth state management in React | Zustand store with `persist` + `hasHydrated` hydration guard |
| No React Context / scattered localStorage | All auth state in `store/authStore.ts`; components never touch localStorage directly |

---

## Open Issues

### 1. CORS Allows All Origins in Dev Config
**Where**: `infrastructure/security/SecurityConfig.java`
**Problem**: CORS is likely configured to allow `*` or `localhost:3000` for development. This config must be locked down to the production domain before any public deployment.
**Impact**: In production, an unrestricted CORS policy allows any website to make credentialed API calls on behalf of authenticated users.
**Fix**: Set `setAllowedOrigins(List.of("https://your-production-domain.com"))` in the prod profile.

---

### 2. JWT Secret From Config (Not KMS)
**Where**: `backend/src/main/resources/application.yml`
**Problem**: JWT signing secret is a static string in config / environment variable. If the secret leaks, all tokens can be forged.
**Impact**: Acceptable for a portfolio project; unacceptable for production financial or health data.
**Fix**: Use AWS KMS, HashiCorp Vault, or GCP Secret Manager for secret rotation in production.

---

### 3. No E2E or Component Tests for Frontend
**Where**: `app/`, `components/`
**Problem**: All frontend testing relies on manual browser verification. No Playwright tests, no Vitest component tests.
**Impact**: Regressions in auth flow or booking UI won't be caught automatically in CI.
**Fix**: Add Playwright E2E tests for the login → book → cancel flow; Vitest for `TimeSlotCard` and `BookingModal`.

---

### 4. Kafka Consumer Not Implemented
**Where**: `adapter/out/kafka/KafkaOutboxPublisher.java`
**Problem**: The Transactional Outbox publishes `booking-events` to Kafka. There is no consumer service that actually processes these events (e.g., send confirmation email, update analytics).
**Impact**: Events are published but go nowhere — the outbox pattern is present but incomplete.
**Fix**: Add a consumer (could be a separate Spring Boot microservice or a `@KafkaListener` in the same app) to handle `BookingEvent` payloads.

---

### 5. Refresh Token Stored in Zustand Persist (localStorage)
**Where**: `store/authStore.ts`
**Problem**: If the refresh token is stored in localStorage, it's accessible to any JavaScript on the page (XSS risk).
**Impact**: XSS attack can steal refresh tokens and maintain persistent session access.
**Fix**: Store refresh tokens in `HttpOnly` cookies (requires backend to set `Set-Cookie` header). Access tokens in memory only.

---

### 6. No Pagination on `/api/timeslots`
**Where**: `TimeSlotController.java`, `bookingService.ts`
**Problem**: All 24 time slots for a date are returned in one response. Fine for now, but won't scale if slot granularity increases.
**Impact**: Low — 24 slots is small. Noted for completeness.

---

### 7. Outbox Scheduler Polling Interval Is Fixed
**Where**: `KafkaOutboxPublisher.java` (`@Scheduled`)
**Problem**: Fixed polling interval (e.g., every 5 seconds) creates latency floor for event delivery.
**Impact**: Booking confirmation events may be delayed by up to the polling interval.
**Fix**: Use `pg_notify` + LISTEN/NOTIFY or reduce polling interval for near-real-time delivery.
