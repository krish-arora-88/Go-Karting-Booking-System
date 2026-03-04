# Go Karting Booking System — Memory

## Project Identity
- Hybrid project: legacy Java Swing GUI + modern Next.js frontend + Spring Boot backend
- Built in phases; later phases added web stack on top of original Java app
- Academic/portfolio project with known design debt

## Tech Stack
- **Frontend**: Next.js 14 (App Router), React 18, TypeScript 5, Tailwind CSS 3
- **Backend**: Spring Boot 3.2, Java 17, Maven, Spring Security + JWT (JJWT 0.12.3)
- **Legacy**: Java Swing GUI (src/), JUnit 5 tests
- **Persistence**: JSON flat files (Bookings.json, loginData.json) — no database

## Key Paths
- Legacy Java models: `src/main/model/` (Event, TimeSlot, EventLog)
- Legacy Java UI: `src/main/ui/` (Main, LandingPageUI, MainMenu, User)
- Legacy tests: `src/test/model/`
- Spring Boot backend: `backend/src/main/java/com/gokarting/`
- Next.js frontend: `app/`, `components/`, `services/`, `lib/`
- Data files: `Bookings.json`, `loginData.json` (root dir)

## Critical Facts
- Booking API endpoints (`/api/bookings/*`) are NOT implemented in the backend — only auth endpoints exist
- Frontend BookingService references these missing endpoints
- Hardcoded absolute path in LandingPageUI: `/Users/krish/IdeaProjects/...`
- Legacy User.java uses plaintext password comparison (security issue)
- Spring Boot CORS allows all origins (`*`)
- EventLog is a Singleton but NOT thread-safe

## Test Coverage
- 18 unit tests total, all for legacy Java models only
- Zero tests for: Spring Boot controllers/services, React components, API integration

## Detailed Docs
- [architecture.md](architecture.md) — full stack diagram, data flows, class responsibilities
- [design-issues.md](design-issues.md) — all code smells and design problems, ranked by severity
- [codebase-map.md](codebase-map.md) — complete file manifest with descriptions
