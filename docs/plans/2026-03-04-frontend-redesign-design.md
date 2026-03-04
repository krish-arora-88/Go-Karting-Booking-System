# Frontend Redesign: "RACE CONTROL" — Tron/Synthwave Arcade

**Date:** 2026-03-04
**Approach:** A — Tron/Synthwave Arcade (Fun & Playful, Neon Cyan palette)

---

## Design System

### Fonts (Google Fonts via `<link>` in layout.tsx)
- **Display / numbers:** Orbitron (700, 900) — headers, times, CTAs
- **Body / data:** DM Mono (400, 500) — labels, capacity, metadata

### Color Palette (CSS custom properties in globals.css)
```css
--bg:       #060614    /* deep navy-black background */
--surface:  #0d0d2b    /* card / panel surfaces */
--border:   #00CFFF33  /* cyan glass border (20% opacity) */
--cyan:     #00CFFF    /* primary neon — available state, CTAs */
--pink:     #FF2D6B    /* secondary neon — booked state, cancel */
--green:    #7FFF00    /* available status dot */
--dim:      #4a4a7a    /* muted / disabled text */
--white:    #E8F4FF    /* near-white body text */
```

### Motifs
- **Perspective grid floor:** CSS `perspective` + `rotateX(60deg)` on a grid div; infinite `translateZ` animation (~20s loop) scrolling toward the viewer. No canvas.
- **Scanline overlay:** Fixed pseudo-element with `repeating-linear-gradient` of semi-transparent horizontal lines, `pointer-events: none`.
- **Neon glow:** `box-shadow: 0 0 8px var(--cyan), 0 0 20px var(--cyan)40` on focused/active elements.
- **Corner brackets:** CSS `::before` / `::after` on cards — 8px L-shaped cyan corners at each corner using `border-top`/`border-left` etc.
- **Dashed ticket border:** `border: 1px dashed var(--cyan)` at 40% opacity for TimeSlotCard.

---

## Pages & Components

### 1. Landing Page (`app/page.tsx`)

**Layout:** Single full-viewport scene. No photo background. No modal.

**Structure:**
- Top ~60%: dark `--bg` with scanline overlay. Logo (◈ APEX RACING, Orbitron) top-left. Centered inline auth terminal card.
- Bottom ~40%: perspective grid floor, infinite scroll-toward-viewer animation.

**Auth terminal card:**
- `--surface` background, 1px `--cyan` border, corner bracket decorations.
- Inline tabs: "SIGN IN" / "NEW PLAYER" — toggle between login and signup states.
- Signup adds confirm password field with CSS height transition.
- Input focus: cyan bottom-border highlight + subtle glow.
- Buttons: "ENTER RACE ▶" (filled `--cyan`, Orbitron) + "NEW PLAYER +" (ghost `--pink`).

**Replaces:** Full photo-bg + AuthModal pattern entirely.

---

### 2. Auth Modal (`components/auth/AuthModal.tsx`)

**Eliminated.** Auth is now inline on the landing page. The `AuthModal` component is removed and its logic merged into the landing page component.

---

### 3. Dashboard (`app/dashboard/page.tsx`)

**Layout:** Two-column. Narrow sidebar (~200px) + main content area.

**Sidebar:**
- Logo mark top.
- Two nav items: "▶ SLOTS" and "● BOOKINGS". Active state: full-width left cyan bar + text glow. Inactive: dim text.
- Bottom: username + logout icon (eject `▶╗` symbol via Lucide `LogOut`).

**Header bar:**
- App name "RACE CONTROL" in Orbitron left.
- Pulsing cyan dot (replaces `RefreshCw` spinner) when `slotsFetching` is true.

**Date strip:**
- Horizontal scrollable row of 7 day chips (centered on today).
- Each chip: weekday abbrev + date number. Selected: filled `--cyan` glow. Unselected: ghost `--border`.
- Replaces `<input type="date">`. Computes dates client-side with `date-fns`.

**Content grid:**
- Same `grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4` layout.
- Available slots: cyan-themed `TimeSlotCard`.
- My Bookings: pink-themed `TimeSlotCard` with cancel action.

**Empty state:**
- Full-width panel, large dim Orbitron "NO RACES SCHEDULED", subtle grid motif background.

**Mobile:**
- Sidebar collapses to a bottom tab bar (2 icon tabs).
- Date strip: horizontal scroll unchanged.

---

### 4. TimeSlotCard (`components/booking/TimeSlotCard.tsx`)

**Aesthetic:** Perforated race entry pass.

**Structure:**
```
┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
│                           ● OPEN  │
│  09:00                            │
│       — 10:00       ████████░░ 8/10
│                                   │
│          [ BOOK SLOT ▶ ]          │
└ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
```

**Details:**
- Border: `1px dashed` at `--cyan` 40% opacity. Corner brackets via pseudo-elements.
- Time: Large Orbitron hour (`09:00`), smaller dim end time (`— 10:00`).
- Status badge: top-right. `● OPEN` in `--green` (dot pulses via `@keyframes`), `● FULL` in dim red, `● BOOKED` in `--pink`.
- Capacity bar: CSS-only progress bar. `--green` fill on `--surface` track. `8/10` label in DM Mono.
- Hover: `translateY(-2px)`, border opacity 100%, outer glow activates.
- Book button: Full-width, filled `--cyan`, Orbitron "BOOK SLOT ▶". Disabled → dim, text "RACE FULL".
- Booked variant: Pink border, "✕ CANCEL" ghost button in `--pink`. Same component, `type` prop controls.

---

## File Changes

| File | Action |
|------|--------|
| `app/globals.css` | Replace with Tron design system (CSS vars, fonts, utilities) |
| `app/layout.tsx` | Add Google Fonts `<link>` (Orbitron + DM Mono) |
| `app/page.tsx` | Full rewrite — inline auth, perspective grid, scanline |
| `app/dashboard/page.tsx` | Full rewrite — sidebar layout, date strip |
| `components/auth/AuthModal.tsx` | Delete — logic merged into landing page |
| `components/booking/TimeSlotCard.tsx` | Full rewrite — race entry pass aesthetic |
| `components/ui/Button.tsx` | Update — Tron-styled variants (filled cyan, ghost pink) |

---

## Constraints

- Zero new npm packages — only Google Fonts (CDN link) and existing deps (Lucide, TanStack Query, Zustand, date-fns).
- All existing API logic (SSE, mutations, auth flow) preserved exactly.
- Fully responsive: sidebar → bottom tab bar on mobile.
- No canvas, WebGL, or heavy animation libraries — CSS-only effects.
