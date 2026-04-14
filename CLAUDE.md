# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Production-grade Go Karting Booking System — a portfolio project showcasing modern distributed systems. Frontend is a Next.js 14 app deployed to Vercel; backend is a Spring Boot 3 service deployed to Fly.io.

## Commands

### Frontend
```bash
npm run dev          # Next.js dev server → http://localhost:3000
npm run build        # Production build
npm run lint         # ESLint
npx tsc --noEmit     # TypeScript type check
```

### Backend
```bash
cd backend && ./mvnw spring-boot:run          # Run with local infra
cd backend && ./mvnw test                     # All tests (needs Docker for Testcontainers)
cd backend && ./mvnw test -Dtest=BookingControllerIT   # Single test class
cd backend && ./mvnw clean install -DskipTests         # Build without tests
```

### Infrastructure
```bash
docker compose up    # All infra: postgres, redis, kafka, prometheus, grafana
```

Ports: API `:8080`, Next.js `:3000`, Grafana `:3001`, Prometheus `:9090`, Kafka UI `:8081`, Swagger UI at `http://localhost:8080/swagger-ui.html`

## Architecture

### Hexagonal (Ports & Adapters)

```
domain/           ← Pure Java, zero framework dependencies (ArchUnit-enforced)
  model/          ← Booking, TimeSlot, User, OutboxEvent
  port/in/        ← Use case interfaces (BookSlotUseCase, etc.)
  port/out/       ← Repository/publisher interfaces
  event/          ← BookingEvent, SlotAvailabilityChangedEvent

application/      ← Use case implementations (BookSlotService, etc.)
                    Inject port interfaces only, never concrete adapters

adapter/
  in/web/         ← REST controllers + DTOs (Spring MVC)
  in/kafka/       ← Kafka consumers
  out/persistence/← JPA entities + Spring Data repos + adapter impls
  out/kafka/      ← Kafka event publisher adapter
  out/cache/      ← Redis cache adapter

infrastructure/
  security/       ← JWT filter, rate limiting (Bucket4j), token blacklist
  outbox/         ← OutboxPoller scheduler
  metrics/        ← BookingMetrics (Micrometer) — implements BookingMetricsPort
  logging/        ← JSON structured logging config
  sse/            ← Server-Sent Events
```

**Key rule**: `application/` services inject `BookingMetricsPort`, never `BookingMetrics` directly. ArchUnit tests enforce all layer boundaries — run them before touching package structure.

### Critical Patterns

**Transactional Outbox**: `BookSlotService` saves a `Booking` + `OutboxEvent` in one transaction. `OutboxPoller` (polls every 500ms) reads unpublished events and publishes to Kafka. Never publish Kafka events directly from a service method.

**Optimistic Locking**: `TimeSlotEntity` has `@Version`. Double-booking races throw `OptimisticLockException`, caught and converted to a 409 Conflict.

**Group Booking**: One `Booking` record per group. `racer_count` drains slot capacity (uses SUM, not COUNT). `racer_names` is a JSONB array. The unique constraint is on `(time_slot_id, booked_by)`.

**JWT Flow**: Access tokens expire in 15 min; refresh tokens in 7 days. Refresh tokens are stored in Redis. On logout, the access token is blacklisted in Redis via `TokenBlacklistService`. `RefreshTokenService` handles rotation.

**Idempotency**: `BookingController` reads `X-Idempotency-Key` header to prevent duplicate bookings on retry.

### Frontend State

**Zustand hydration guard**: `authStore.ts` has a `hasHydrated` flag set via `onRehydrateStorage`. The dashboard page gates auth redirects on `hasHydrated === true` to prevent false logouts on page refresh.

**TanStack Query**: Dashboard uses `useQuery` for time slots and `useMutation` for bookings. Query keys include the selected date.

### Database Schema (Flyway V1–V9)

Flyway manages all schema changes. Hibernate is set to `validate` only — never change `ddl-auto`. Current migrations: users → time_slots → bookings → outbox_events → indexes → seed slots → status type fix → racer details → booking unique constraint fix.

## Important Build Notes

- **Lombok 1.18.38** is explicitly overridden in `pom.xml` (required for Java 21 + Homebrew Maven compatibility)
- `maven-compiler-plugin` includes Lombok in `annotationProcessorPaths` explicitly
- `flyway-database-postgresql` is NOT included — Flyway 9.x has built-in PostgreSQL support
- Integration tests use Testcontainers (requires Docker running locally)
- `application-no-kafka.yml` profile exists for running without Kafka (skips outbox polling)

## Frontend Design System

Dark Tron/synthwave aesthetic. CSS variables in `globals.css`: `--bg #060614`, `--surface #0d0d2b`, `--cyan #00CFFF`, `--pink #FF2D6B`.

**Font usage**: Use `font-orbitron` and `font-mono` Tailwind classes — do NOT use `font-['Orbitron']`. Fonts are loaded via `next/font/google` which generates hashed CSS variable names.

**Tailwind arbitrary values with CSS vars**: `bg-[var(--cyan)]` not `bg-[#00CFFF]`.

Key CSS classes: `.tron-input`, `.bracket-corner .tl/.tr/.bl/.br`, `.grid-floor-container`, `.capacity-bar-track/.capacity-bar-fill`, `.fetch-dot`, `.pulse-dot`.

Button variants in `components/ui/Button.tsx`: `cyan` (default), `pink-ghost`, `dim-ghost`, `outline`.

## Terraform (IaC)

### Commands
```bash
cd infra

# First-time setup (bootstrap S3 state bucket + DynamoDB lock table first)
terraform init -backend-config=environments/dev/backend.tfvars

# Plan / Apply
terraform plan  -var-file=environments/dev/terraform.tfvars
terraform apply -var-file=environments/dev/terraform.tfvars

# Lint & security scan (run before every PR)
tflint --init && tflint --recursive
checkov -d . --framework terraform --quiet

# Format (also runs automatically via Claude hook on every .tf file write)
terraform fmt -recursive
```

### Module Layout
```
infra/
├── main.tf / variables.tf / outputs.tf / backend.tf   ← root module
├── modules/
│   ├── networking/   ← VPC, subnets, IGW, NAT, route tables
│   ├── ecs/          ← ECS cluster, task def, service, ALB
│   ├── database/     ← RDS PostgreSQL, subnet group, Secrets Manager
│   ├── cache/        ← ElastiCache Redis, subnet group
│   └── iam/          ← Task execution role + task role (kept separate)
└── environments/
    ├── dev/          ← small instances, single AZ
    └── prod/         ← multi-AZ RDS, larger compute
```

### IAM Role Separation (critical for interviews)
Two distinct ECS roles — never merge them:
- **Task Execution Role** (ECS agent): ECR pull, CloudWatch logs, Secrets Manager read
- **Task Role** (application code): only what the app actually calls (specific S3 bucket, nothing else)

### Networking Rules
- ALB in public subnets — only resource with `0.0.0.0/0` ingress on 80/443
- ECS tasks, RDS, ElastiCache all in private subnets
- Security groups follow least-privilege: each tier only accepts from the tier above it

### GitHub Actions CI
Three jobs in `.github/workflows/terraform.yml`:
1. **validate** — `fmt -check`, `validate`, `tflint`, `checkov` (every push)
2. **plan** — `terraform plan`, posts output as PR comment (PRs only)
3. **apply** — `terraform apply` (merge to main only)

Required GitHub secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `TF_BACKEND_BUCKET`, `TF_BACKEND_LOCK_TABLE`

## Environment Variables

Backend reads from env (with defaults for local dev): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `KAFKA_SERVERS`, `JWT_SECRET`, `CORS_ORIGINS`.

Frontend: `NEXT_PUBLIC_API_URL` (defaults to `http://localhost:8080`).
