# Codebase Map

Complete file manifest with purpose and status notes.

---

## Frontend — Next.js App (`app/`, `components/`, `services/`, `lib/`)

### Pages
| File | Purpose | Status |
|------|---------|--------|
| [app/page.tsx](../app/page.tsx) | Landing page with Sign In / Create Account buttons; triggers AuthModal | Works |
| [app/dashboard/page.tsx](../app/dashboard/page.tsx) | Booking dashboard: available slots tab + my bookings tab; auth-gated | UI works; API calls fail (missing backend) |
| [app/layout.tsx](../app/layout.tsx) | Root layout; loads Inter font; renders Toaster | Works |
| [app/globals.css](../app/globals.css) | Tailwind base styles + CSS variable theme tokens | Works |

### Components
| File | Purpose | Status |
|------|---------|--------|
| [components/ui/Button.tsx](../components/ui/Button.tsx) | Reusable button with variants (default/outline/ghost) and sizes | Works |
| [components/auth/AuthModal.tsx](../components/auth/AuthModal.tsx) | Login/signup modal; form validation; calls authService | Works |
| [components/booking/TimeSlotCard.tsx](../components/booking/TimeSlotCard.tsx) | Renders one time slot with book/cancel action | Works (renders correctly) |

### Services
| File | Purpose | Status |
|------|---------|--------|
| [services/authService.ts](../services/authService.ts) | JWT auth: login, register, logout, isAuthenticated, localStorage management | Works |
| [services/bookingService.ts](../services/bookingService.ts) | Booking CRUD: getTimeSlots, bookSlot, cancelBooking, getUserBookings | Broken — backend endpoints missing |

### Utilities
| File | Purpose |
|------|---------|
| [lib/utils.ts](../lib/utils.ts) | `cn()` helper: combines clsx + tailwind-merge for conditional class names |

---

## Backend — Spring Boot (`backend/`)

### Application
| File | Purpose | Status |
|------|---------|--------|
| [backend/src/main/java/com/gokarting/BookingSystemApplication.java](../backend/src/main/java/com/gokarting/BookingSystemApplication.java) | Spring Boot entry point (`@SpringBootApplication`) | Works |

### Controllers
| File | Endpoints | Status |
|------|-----------|--------|
| [backend/src/main/java/com/gokarting/controller/AuthController.java](../backend/src/main/java/com/gokarting/controller/AuthController.java) | POST `/api/auth/login`, POST `/api/auth/register` | Works |
| _(missing)_ | GET `/api/bookings/timeslots`, POST `/api/bookings/book`, POST `/api/bookings/cancel`, GET `/api/bookings/user` | **Not implemented** |

### Services
| File | Purpose | Status |
|------|---------|--------|
| [backend/src/main/java/com/gokarting/service/AuthService.java](../backend/src/main/java/com/gokarting/service/AuthService.java) | Loads/saves `loginData.json`; validates credentials; issues JWT | Works; not thread-safe on file writes |

### Models & DTOs
| File | Purpose |
|------|---------|
| [backend/src/main/java/com/gokarting/model/User.java](../backend/src/main/java/com/gokarting/model/User.java) | User POJO: username + password (BCrypt hash) |
| [backend/src/main/java/com/gokarting/dto/AuthRequest.java](../backend/src/main/java/com/gokarting/dto/AuthRequest.java) | Request body for login/register |
| [backend/src/main/java/com/gokarting/dto/AuthResponse.java](../backend/src/main/java/com/gokarting/dto/AuthResponse.java) | Response body: token + username |

### Configuration & Security
| File | Purpose | Notes |
|------|---------|-------|
| [backend/src/main/java/com/gokarting/config/SecurityConfig.java](../backend/src/main/java/com/gokarting/config/SecurityConfig.java) | Spring Security config; CORS; JWT filter chain; BCryptEncoder bean | CORS allows `*` — overly permissive |
| _(referenced, not shown)_ `security/JwtTokenProvider.java` | Generates and validates JWT tokens | Source not visible |
| _(referenced, not shown)_ `security/JwtAuthenticationFilter.java` | Validates JWT on incoming requests | Source not visible |

### Build
| File | Purpose |
|------|---------|
| [backend/pom.xml](../backend/pom.xml) | Maven config; Spring Boot 3.2, JJWT 0.12.3, Java 17 |

---

## Legacy Java Swing (`src/`)

### Models
| File | Purpose | Notes |
|------|---------|-------|
| [src/main/model/Event.java](../src/main/model/Event.java) | Immutable event record: description + timestamp | Well-designed; final fields |
| [src/main/model/EventLog.java](../src/main/model/EventLog.java) | Singleton event store; Iterable | Not thread-safe |
| [src/main/model/TimeSlot.java](../src/main/model/TimeSlot.java) | Time slot: times, capacity, bookedRacers; JSON constructor | Tightly coupled to EventLog |

### UI
| File | Purpose | Notes |
|------|---------|-------|
| [src/main/ui/Main.java](../src/main/ui/Main.java) | Entry point; creates LandingPageUI | Trivial |
| [src/main/ui/LandingPageUI.java](../src/main/ui/LandingPageUI.java) | Swing landing screen with background image | Hardcoded absolute image path |
| [src/main/ui/User.java](../src/main/ui/User.java) | Login/signup UI + JSON credential persistence | Plaintext password comparison |
| [src/main/ui/MainMenu.java](../src/main/ui/MainMenu.java) | God object: all booking UI + logic + file I/O | 310+ lines; 38-case switch |

### Tests (JUnit 5)
| File | Tests | Coverage |
|------|-------|---------|
| [src/test/model/EventTest.java](../src/test/model/EventTest.java) | 6 tests: getDescription, getTimeStamp, equals, hashCode, toString | Event model only |
| [src/test/model/TimeSlotTest.java](../src/test/model/TimeSlotTest.java) | 10 tests: JSON constructor, isAvailable, bookSlot, cancelSlot, getters, toString | TimeSlot model only |
| [src/test/model/EventLogTest.java](../src/test/model/EventLogTest.java) | 2 tests: logEvent, clear | EventLog model only |

---

## Data Files

| File | Contents | Written By |
|------|----------|-----------|
| [Bookings.json](../Bookings.json) | Array of 24 time slot objects with `bookedRacers` arrays | Legacy MainMenu.java |
| [loginData.json](../loginData.json) | Array of user objects with BCrypt-hashed passwords | Spring Boot AuthService / Legacy User.java |
| [data/bookings.json](../data/bookings.json) | Duplicate or alternate bookings file (unused?) | Unknown |
| [data/users.json](../data/users.json) | Duplicate or alternate users file (unused?) | Unknown |
| [data/logs.json](../data/logs.json) | Log entries file (unused?) | Unknown |

---

## Configuration

| File | Purpose |
|------|---------|
| [package.json](../package.json) | Frontend dependencies: Next.js 14, React 18, TypeScript, Tailwind, lucide-react, react-hot-toast |
| [tsconfig.json](../tsconfig.json) | TypeScript config for Next.js (strict mode enabled) |
| [tailwind.config.js](../tailwind.config.js) | Tailwind config with custom color tokens |
| [next.config.js](../next.config.js) | Next.js config |
| [postcss.config.js](../postcss.config.js) | PostCSS for Tailwind processing |
| [vercel.json](../vercel.json) | Vercel deployment config |
| [components.json](../components.json) | shadcn/ui config (component library setup) |
| [.gitignore](.gitignore) | Git exclusions |

---

## Inventory Summary

| Category | Count |
|----------|-------|
| Frontend TypeScript/TSX files | 10 |
| Spring Boot Java files (visible) | 7 |
| Legacy Swing Java files | 7 |
| JUnit test files | 3 |
| JSON data files | 5 |
| Config files | 8 |
| **Total source files** | **~40** |
| Unit test methods | 18 (models only) |
| Implemented API endpoints | 2 of 6 |
