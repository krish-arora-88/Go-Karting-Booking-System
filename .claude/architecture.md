# Architecture Overview

## System Layers

```
┌─────────────────────────────────────────────────────────┐
│               Next.js Frontend (Port 3000)               │
│  app/page.tsx → AuthModal → /dashboard/page.tsx          │
│  services/authService.ts  services/bookingService.ts     │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP (JWT Bearer tokens)
┌──────────────────────▼──────────────────────────────────┐
│           Spring Boot Backend (Port 8080)                │
│  /api/auth/login   /api/auth/register                    │
│  /api/bookings/**  ← NOT IMPLEMENTED                     │
│  AuthService ← loginData.json                            │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│           Legacy Java Swing GUI (standalone)             │
│  Main → LandingPageUI → User / MainMenu                  │
│  MainMenu ← Bookings.json / loginData.json               │
└─────────────────────────────────────────────────────────┘
```

The two systems (web stack and Java Swing) share the same JSON data files but have no runtime connection.

---

## Frontend (Next.js)

### Pages
| File | Route | Responsibility |
|------|-------|----------------|
| `app/page.tsx` | `/` | Landing page; triggers AuthModal |
| `app/dashboard/page.tsx` | `/dashboard` | Booking management (authenticated) |
| `app/layout.tsx` | (root) | Wraps all pages; adds Toaster |

### Components
| File | Responsibility |
|------|---------------|
| `components/ui/Button.tsx` | Reusable button (variants: default/outline/ghost) |
| `components/auth/AuthModal.tsx` | Login/signup form modal with validation |
| `components/booking/TimeSlotCard.tsx` | Single time slot card (available or booked) |

### Services
| File | Responsibility |
|------|---------------|
| `services/authService.ts` | JWT auth; reads/writes localStorage |
| `services/bookingService.ts` | Booking CRUD via REST; attaches Bearer token |

### Authentication Flow
1. User submits AuthModal → `authService.login/register()`
2. POST `/api/auth/login` or `/api/auth/register`
3. JWT token returned → stored in `localStorage`
4. Dashboard reads token on load; redirects to `/` if absent

### Booking Flow (Partially Broken)
1. Dashboard calls `bookingService.getTimeSlots()` → GET `/api/bookings/timeslots`
2. Backend endpoint **does not exist** → request fails
3. Book/Cancel POSTs to `/api/bookings/book` and `/api/bookings/cancel` — also missing

---

## Backend (Spring Boot)

### Package Structure
```
com.gokarting/
├── BookingSystemApplication.java   — entry point
├── controller/
│   └── AuthController.java         — POST /api/auth/login, /register
├── service/
│   └── AuthService.java            — credential validation, JWT issuance
├── model/
│   └── User.java                   — username + password POJO
├── dto/
│   ├── AuthRequest.java            — login/register request body
│   └── AuthResponse.java           — token + username response
├── config/
│   └── SecurityConfig.java         — Spring Security, CORS, JWT filter chain
└── security/                       — JwtTokenProvider, JwtAuthenticationFilter (not shown)
```

### Implemented Endpoints
| Method | Path | Auth Required | Status |
|--------|------|--------------|--------|
| POST | `/api/auth/login` | No | ✅ Works |
| POST | `/api/auth/register` | No | ✅ Works |
| GET | `/api/health` | No | ✅ (default) |
| GET | `/api/bookings/timeslots` | Yes | ❌ Missing |
| POST | `/api/bookings/book` | Yes | ❌ Missing |
| POST | `/api/bookings/cancel` | Yes | ❌ Missing |
| GET | `/api/bookings/user` | Yes | ❌ Missing |

### Data Persistence
- `loginData.json` — array of `{username, password}` objects (BCrypt hashed)
- `Bookings.json` — array of 24 time slot objects with `bookedRacers` arrays
- Both files read/written directly from Java (no ORM, no DB)
- No transactions; concurrent writes can corrupt files

---

## Legacy Java Swing

### Class Responsibilities
| Class | Extends | Responsibility |
|-------|---------|----------------|
| `Main` | — | Entry point; creates LandingPageUI |
| `LandingPageUI` | JPanel | Landing screen; opens User dialog |
| `User` | JFrame | Login/signup UI + credential persistence |
| `MainMenu` | JFrame | **God object**: all booking logic + UI + file I/O |
| `TimeSlot` | — | Time slot model; calls EventLog directly |
| `Event` | — | Immutable event record (description + timestamp) |
| `EventLog` | — | Singleton event store; iterable |

### Design Pattern Usage
| Pattern | Where | Notes |
|---------|-------|-------|
| Singleton | `EventLog` | Not thread-safe |
| Immutable Value Object | `Event` | Correct use of `final` fields |
| Observer (informal) | EventLog iteration | Manual iteration, no formal listener |

### Event Logging Flow
```
TimeSlot.bookSlot(name)
  → EventLog.getInstance().logEvent(new Event("Booked: " + name))

MainMenu.logs()
  → iterates EventLog.getInstance()
  → displays in JTextArea
```

---

## Data Models

### TimeSlot (canonical shape in Bookings.json)
```json
{
  "startTime": "12:00",
  "endTime": "12:30",
  "capacity": 10,
  "bookedRacers": ["Alice", "Bob"]
}
```

### User (canonical shape in loginData.json)
```json
{
  "username": "krish",
  "password": "$2a$10$..."
}
```

---

## Deployment

| Component | Platform | Config File |
|-----------|----------|-------------|
| Frontend | Vercel | `vercel.json` |
| Backend | Standalone JAR | `backend/pom.xml` |
| Legacy GUI | Local only | — |

Environment variable `BACKEND_URL` must be set for frontend → backend communication.
