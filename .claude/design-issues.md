# Design Issues & Code Smells

Ranked by severity. Critical issues block correctness or security; moderate issues hurt maintainability or scalability; minor issues are style/hygiene problems.

---

## Critical

### 1. Booking API Endpoints Are Not Implemented
**Where**: Spring Boot backend (`backend/src/main/java/com/gokarting/controller/`)
**Problem**: `BookingService.ts` calls four endpoints that have no corresponding controller:
- `GET /api/bookings/timeslots`
- `POST /api/bookings/book`
- `POST /api/bookings/cancel`
- `GET /api/bookings/user`

Only `AuthController` exists. The entire booking flow in the web UI is broken.

---

### 2. Plaintext Password Comparison in Legacy `User.java`
**Where**: `src/main/ui/User.java`, `actionPerformed()`
**Problem**: Login compares `enteredPassword.equals(storedPassword)` directly. Passwords are stored and compared as plaintext strings.
**Impact**: Anyone with file access to `loginData.json` (as written by the legacy app) gets all credentials.
**Note**: The Spring Boot backend uses BCrypt correctly; this only affects the Swing GUI path.

---

### 3. Hardcoded Absolute Path for Image Asset
**Where**: `src/main/ui/LandingPageUI.java`
**Problem**: Background image loaded from `/Users/krish/IdeaProjects/project_w0a4z/GoKartingPhoto.jpeg`.
**Impact**: App fails to launch on any machine other than the original developer's system.

---

### 4. Overly Permissive CORS
**Where**: `backend/src/main/java/com/gokarting/config/SecurityConfig.java`
**Problem**: `setAllowedOriginPatterns(Arrays.asList("*"))` allows any origin to make credentialed requests.
**Impact**: Opens the API to cross-site request forgery from any domain in browsers.

---

## Moderate

### 5. God Object — `MainMenu.java`
**Where**: `src/main/ui/MainMenu.java` (310+ lines)
**Problem**: A single class handles:
- All UI rendering (GridLayout, button panels, slot display)
- Business logic (booking, cancellation validation)
- File I/O (reading/writing Bookings.json)
- Event log display
- Input collection (JOptionPane dialogs)

**Impact**: Untestable, hard to modify, violates Single Responsibility Principle.
**Fix**: Extract `BookingRepository`, `BookingService`, and separate UI panels.

---

### 6. Tight Coupling — `TimeSlot` calls `EventLog` directly
**Where**: `src/main/model/TimeSlot.java`, `bookSlot()` and `cancelSlot()`
**Problem**: Model directly calls `EventLog.getInstance()` — a global singleton — as a side effect of normal business operations.
**Impact**: Impossible to test `TimeSlot` without the EventLog receiving events. Creates hidden dependencies.
**Fix**: Pass an `EventLog` reference via constructor injection, or use a callback/listener.

---

### 7. `EventLog` Is Not Thread-Safe
**Where**: `src/main/model/EventLog.java`
**Problem**: `events` is a plain `ArrayList`. `logEvent()` and `clear()` are not synchronized.
**Impact**: Under concurrent load (web backend), events can be lost or corrupted.
**Fix**: Use `CopyOnWriteArrayList` or add `synchronized` blocks.

---

### 8. Missing Booking Controller Means Frontend Is Dead Code
**Where**: `services/bookingService.ts`, `app/dashboard/page.tsx`
**Problem**: The entire dashboard booking UI (time slot display, book button, cancel button, "My Bookings" tab) makes API calls that always fail with 404 or 401.
**Impact**: The web app's primary feature does not work.

---

### 9. File-Based Persistence With No Concurrency Safety
**Where**: `backend/src/main/java/com/gokarting/service/AuthService.java`, `src/main/ui/MainMenu.java`
**Problem**: JSON files are read entirely into memory, modified, and written back. No locking, no transactions.
**Impact**: Concurrent requests to the Spring Boot backend will cause race conditions and potential data corruption in `loginData.json`.

---

### 10. Mutable List Exposed from `TimeSlot`
**Where**: `src/main/model/TimeSlot.java`, `getBookedRacers()`
**Problem**: Returns the internal `ArrayList<String>` directly. Callers can mutate it without going through `bookSlot()`/`cancelSlot()`, bypassing event logging.
**Fix**: Return `Collections.unmodifiableList(bookedRacers)`.

---

### 11. No Input Validation on Auth DTOs
**Where**: `backend/src/main/java/com/gokarting/dto/AuthRequest.java`
**Problem**: No `@NotNull`, `@NotBlank`, or length constraints. Any string (including empty, null, or extremely long values) is accepted.
**Impact**: Potential null pointer exceptions in `AuthService`; no protection against malformed requests.

---

### 12. `AuthController` Returns Generic 400 on All Errors
**Where**: `backend/src/main/java/com/gokarting/controller/AuthController.java`
**Problem**: All exceptions are caught and returned as `400 Bad Request` with no structured error body.
**Impact**: Frontend cannot distinguish "wrong password" from "user doesn't exist" from "server error".

---

### 13. No Authentication State Management in React
**Where**: `app/dashboard/page.tsx`, `services/authService.ts`
**Problem**: Auth state (token, user) is read directly from `localStorage` in multiple components. No React Context or store.
**Impact**: Adding a new authenticated page requires duplicating the `localStorage` check. Logout in one tab doesn't propagate to others.

---

### 14. Unchecked Cast in `TimeSlot` JSON Constructor
**Where**: `src/main/model/TimeSlot.java`
**Problem**: `for (Object o : bookedRacersArray)` then casts `o` to `String` without a type check.
**Impact**: `ClassCastException` at runtime if the JSON contains non-string values in the `bookedRacers` array.

---

### 15. Test Coverage Is Nearly Zero for the Web Stack
**Where**: No test files exist under `app/`, `components/`, `services/`, or `backend/src/test/`
**Problem**: 18 unit tests exist, all for three legacy Java model classes.
**Impact**: Every Sprint Boot controller, service, React component, and API flow is completely untested.

---

## Minor

### 16. Magic Numbers in `MainMenu`
**Where**: `src/main/ui/MainMenu.java`, constructor
**Problem**: `24` (slot count), `10` (capacity), `30` (slot duration minutes) are inlined.
**Fix**: Extract as named constants.

---

### 17. `LandingPageUI` Constructor Named `landingPage()`
**Where**: `src/main/ui/LandingPageUI.java`
**Problem**: The constructor is a non-standard method `landingPage()` called explicitly after `new LandingPageUI()`. Misleading and error-prone.

---

### 18. Static Fields Mixed With Instance Fields in `LandingPageUI`
**Where**: `src/main/ui/LandingPageUI.java`
**Problem**: Some UI components declared `static`, making it impossible to have two simultaneous instances and causing memory leaks.

---

### 19. `actionPerformed` in `MainMenu` Is a 38-Case Switch Statement
**Where**: `src/main/ui/MainMenu.java`, `actionPerformed()`
**Problem**: All button click handling in one 270+ line method.
**Fix**: Delegate each case to a dedicated handler method; or use lambdas on button listeners.

---

### 20. No Environment Profiles or External Configuration
**Where**: Backend has no `application.properties` or `application.yml`
**Problem**: Hard-coded file paths (`"loginData.json"`, `"Bookings.json"`) and default Spring Boot settings used everywhere.
**Fix**: Externalize via `application.properties` with Spring `@Value` injection.

---

### 21. `authService.register()` Auto-Logs In After Registration
**Where**: `services/authService.ts`, `register()`
**Problem**: After a successful registration, it immediately calls `login()` without telling the user. Silent side effect that's easy to miss when reading the code.

---

### 22. Direct `localStorage` Access Scattered Across Components
**Where**: `app/dashboard/page.tsx`, `services/authService.ts`
**Problem**: `localStorage.getItem('token')` and related calls appear in multiple places.
**Fix**: Centralize all storage access inside `authService` and never call `localStorage` directly in components.
