# Frontend Redesign: Race Control / Tron Arcade Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the generic gray/white SaaS UI with an immersive Tron/synthwave arcade aesthetic across all frontend surfaces.

**Architecture:** All visual layer changes only — zero API logic touched. Auth moves inline to landing page (no modal). Dashboard gains a persistent sidebar and a date-chip strip replacing the native date input. All effects are CSS-only (no canvas, no animation libraries).

**Tech Stack:** Next.js 14 App Router, React 18, TypeScript, Tailwind CSS 3, Google Fonts (Orbitron + DM Mono via CDN link), date-fns (already installed), Lucide React (already installed).

---

## Task 1: Design System — globals.css + layout.tsx

**Files:**
- Modify: `app/globals.css` (full rewrite)
- Modify: `app/layout.tsx`

**Step 1: Replace globals.css entirely**

```css
/* app/globals.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

/* ── Design tokens ───────────────────────────────────── */
:root {
  --bg:      #060614;
  --surface: #0d0d2b;
  --border:  rgba(0, 207, 255, 0.2);
  --cyan:    #00CFFF;
  --pink:    #FF2D6B;
  --green:   #7FFF00;
  --dim:     #4a4a7a;
  --white:   #E8F4FF;
}

/* ── Base ────────────────────────────────────────────── */
html, body {
  background-color: var(--bg);
  color: var(--white);
  font-family: 'DM Mono', monospace;
  min-height: 100vh;
}

/* ── Scanline overlay ────────────────────────────────── */
body::after {
  content: '';
  position: fixed;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 0, 0, 0.04) 2px,
    rgba(0, 0, 0, 0.04) 4px
  );
  pointer-events: none;
  z-index: 9999;
}

/* ── Perspective grid floor ──────────────────────────── */
.grid-floor-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 42%;
  perspective: 220px;
  overflow: hidden;
}

.grid-floor-inner {
  position: absolute;
  width: 200%;
  height: 200%;
  left: -50%;
  background-image:
    linear-gradient(rgba(0, 207, 255, 0.12) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 207, 255, 0.12) 1px, transparent 1px);
  background-size: 60px 60px;
  transform: rotateX(65deg);
  transform-origin: center top;
  animation: gridScroll 2.5s linear infinite;
}

@keyframes gridScroll {
  from { background-position: 0 0; }
  to   { background-position: 0 60px; }
}

/* ── Neon glow utilities ─────────────────────────────── */
.neon-cyan {
  box-shadow: 0 0 8px var(--cyan), 0 0 24px rgba(0, 207, 255, 0.25);
}
.neon-pink {
  box-shadow: 0 0 8px var(--pink), 0 0 24px rgba(255, 45, 107, 0.25);
}
.text-neon-cyan {
  text-shadow: 0 0 10px rgba(0, 207, 255, 0.6);
}

/* ── Corner bracket decoration ───────────────────────── */
.bracket-corner {
  position: absolute;
  width: 10px;
  height: 10px;
  border-color: var(--cyan);
  border-style: solid;
  opacity: 0.7;
}
.bracket-corner.tl { top: 0;    left: 0;  border-width: 1px 0 0 1px; }
.bracket-corner.tr { top: 0;    right: 0; border-width: 1px 1px 0 0; }
.bracket-corner.bl { bottom: 0; left: 0;  border-width: 0 0 1px 1px; }
.bracket-corner.br { bottom: 0; right: 0; border-width: 0 1px 1px 0; }

/* ── Status dot pulse ────────────────────────────────── */
@keyframes dotPulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.3; }
}
.pulse-dot {
  animation: dotPulse 1.6s ease-in-out infinite;
}

/* ── Fetch indicator pulse ───────────────────────────── */
@keyframes fetchPulse {
  0%, 100% { box-shadow: 0 0 4px var(--cyan); opacity: 0.6; }
  50%       { box-shadow: 0 0 12px var(--cyan); opacity: 1; }
}
.fetch-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--cyan);
  animation: fetchPulse 1s ease-in-out infinite;
}

/* ── Sidebar nav active bar ──────────────────────────── */
.nav-item-active {
  border-left: 2px solid var(--cyan);
  color: var(--cyan);
  text-shadow: 0 0 8px rgba(0, 207, 255, 0.5);
  background: rgba(0, 207, 255, 0.05);
}
.nav-item-inactive {
  border-left: 2px solid transparent;
  color: var(--dim);
}
.nav-item-inactive:hover {
  color: var(--white);
  border-left-color: rgba(0, 207, 255, 0.3);
}

/* ── Input field ─────────────────────────────────────── */
.tron-input {
  background: rgba(13, 13, 43, 0.8);
  border: 0;
  border-bottom: 1px solid var(--dim);
  color: var(--white);
  font-family: 'DM Mono', monospace;
  font-size: 0.875rem;
  padding: 0.5rem 0;
  width: 100%;
  outline: none;
  transition: border-color 0.2s;
}
.tron-input:focus {
  border-bottom-color: var(--cyan);
  box-shadow: 0 2px 0 0 rgba(0, 207, 255, 0.3);
}
.tron-input::placeholder {
  color: var(--dim);
}

/* ── Capacity bar ────────────────────────────────────── */
.capacity-bar-track {
  height: 4px;
  background: rgba(74, 74, 122, 0.4);
  border-radius: 2px;
  overflow: hidden;
}
.capacity-bar-fill {
  height: 100%;
  border-radius: 2px;
  background: var(--green);
  box-shadow: 0 0 6px var(--green);
  transition: width 0.4s ease;
}
.capacity-bar-fill.full {
  background: var(--pink);
  box-shadow: 0 0 6px var(--pink);
}
```

**Step 2: Add Google Fonts to layout.tsx**

Read current `app/layout.tsx` first, then add the `<link>` tags inside `<head>` and set `className` on `<html>`:

```tsx
// app/layout.tsx — add inside the returned JSX
// In the <head> section (or use Next.js metadata + link tags):
// Add before </head>:
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
<link
  href="https://fonts.googleapis.com/css2?family=Orbitron:wght@700;900&family=DM+Mono:wght@400;500&display=swap"
  rel="stylesheet"
/>
```

In Next.js App Router, add font links via the layout's `<head>` or via `next/font`. The simplest approach is adding a `<link>` tag directly in the layout `<head>`. Open `app/layout.tsx`, read its current contents, then add the links.

**Step 3: Verify build compiles**

```bash
cd "/Users/krish/Library/Mobile Documents/com~apple~CloudDocs/Projects/Go Karting Booking System"
npm run build 2>&1 | tail -20
```
Expected: no TypeScript errors. CSS changes don't affect build.

**Step 4: Commit**

```bash
git add app/globals.css app/layout.tsx
git commit -m "feat: add Tron design system — CSS vars, grid floor, neon utilities"
```

---

## Task 2: Button Component — Tron Variants

**Files:**
- Modify: `components/ui/Button.tsx`

**Step 1: Read current Button.tsx**

```bash
cat "components/ui/Button.tsx"
```

**Step 2: Rewrite Button with Tron variants**

```tsx
// components/ui/Button.tsx
import { ButtonHTMLAttributes, forwardRef } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'cyan' | 'pink-ghost' | 'dim-ghost'
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = '', variant = 'cyan', children, disabled, ...props }, ref) => {
    const base =
      'inline-flex items-center justify-center gap-2 font-["Orbitron"] text-xs tracking-widest uppercase transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed'

    const variants: Record<string, string> = {
      cyan:
        'bg-[--cyan] text-[--bg] px-6 py-2.5 hover:shadow-[0_0_16px_var(--cyan)] hover:-translate-y-px',
      'pink-ghost':
        'border border-[--pink] text-[--pink] px-6 py-2.5 hover:bg-[--pink] hover:text-[--bg] hover:shadow-[0_0_12px_var(--pink)]',
      'dim-ghost':
        'border border-[--dim] text-[--dim] px-4 py-2 hover:border-[--white] hover:text-[--white]',
    }

    return (
      <button
        ref={ref}
        disabled={disabled}
        className={`${base} ${variants[variant] ?? variants.cyan} ${className}`}
        {...props}
      >
        {children}
      </button>
    )
  }
)

Button.displayName = 'Button'
```

**Step 3: Verify TypeScript**

```bash
npx tsc --noEmit 2>&1 | head -30
```
Expected: no errors.

**Step 4: Commit**

```bash
git add components/ui/Button.tsx
git commit -m "feat: update Button with Tron neon variants (cyan, pink-ghost)"
```

---

## Task 3: TimeSlotCard — Race Entry Pass

**Files:**
- Modify: `components/booking/TimeSlotCard.tsx`

**Step 1: Rewrite TimeSlotCard**

```tsx
// components/booking/TimeSlotCard.tsx
'use client'

import { Button } from '@/components/ui/Button'
import { TimeSlot } from '@/services/bookingService'

interface TimeSlotCardProps {
  timeSlot: TimeSlot
  onBook?: () => void
  onCancel?: () => void
  type: 'available' | 'booked'
  isPending?: boolean
}

export function TimeSlotCard({ timeSlot, onBook, onCancel, type, isPending }: TimeSlotCardProps) {
  const isFull = timeSlot.remaining === 0
  const fillPct = Math.round(((timeSlot.capacity - timeSlot.remaining) / timeSlot.capacity) * 100)

  const borderColor = type === 'booked' ? 'var(--pink)' : 'rgba(0, 207, 255, 0.35)'
  const hoverShadow = type === 'booked' ? 'hover:shadow-[0_0_16px_rgba(255,45,107,0.3)]' : 'hover:shadow-[0_0_16px_rgba(0,207,255,0.3)]'
  const bracketColor = type === 'booked' ? '[--pink]' : '[--cyan]'

  return (
    <div
      className={`relative p-4 transition-all duration-200 hover:-translate-y-0.5 ${hoverShadow}`}
      style={{
        background: 'var(--surface)',
        border: `1px dashed ${borderColor}`,
      }}
    >
      {/* Corner brackets */}
      <span className="bracket-corner tl" style={{ borderColor: type === 'booked' ? 'var(--pink)' : 'var(--cyan)' }} />
      <span className="bracket-corner tr" style={{ borderColor: type === 'booked' ? 'var(--pink)' : 'var(--cyan)' }} />
      <span className="bracket-corner bl" style={{ borderColor: type === 'booked' ? 'var(--pink)' : 'var(--cyan)' }} />
      <span className="bracket-corner br" style={{ borderColor: type === 'booked' ? 'var(--pink)' : 'var(--cyan)' }} />

      {/* Status badge */}
      <div className="flex justify-end mb-2">
        {type === 'available' && !isFull && (
          <span className="flex items-center gap-1 text-[10px] tracking-widest" style={{ color: 'var(--green)' }}>
            <span className="pulse-dot inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--green)' }} />
            OPEN
          </span>
        )}
        {type === 'available' && isFull && (
          <span className="flex items-center gap-1 text-[10px] tracking-widest text-red-400">
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-red-400" />
            FULL
          </span>
        )}
        {type === 'booked' && (
          <span className="flex items-center gap-1 text-[10px] tracking-widest" style={{ color: 'var(--pink)' }}>
            <span className="inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--pink)' }} />
            BOOKED
          </span>
        )}
      </div>

      {/* Time display */}
      <div className="mb-3">
        <div
          className="font-['Orbitron'] font-bold text-2xl leading-none"
          style={{ color: type === 'booked' ? 'var(--pink)' : 'var(--cyan)' }}
        >
          {timeSlot.startTime}
        </div>
        <div className="font-['DM_Mono'] text-xs mt-0.5" style={{ color: 'var(--dim)' }}>
          — {timeSlot.endTime}
        </div>
      </div>

      {/* Capacity bar */}
      <div className="mb-4">
        <div className="capacity-bar-track">
          <div
            className={`capacity-bar-fill ${isFull ? 'full' : ''}`}
            style={{ width: `${fillPct}%` }}
          />
        </div>
        <div className="flex justify-end mt-1">
          <span className="font-['DM_Mono'] text-[10px]" style={{ color: 'var(--dim)' }}>
            {timeSlot.capacity - timeSlot.remaining}/{timeSlot.capacity}
          </span>
        </div>
      </div>

      {/* Action button */}
      {type === 'available' && onBook && (
        <Button
          variant="cyan"
          onClick={onBook}
          disabled={isFull || isPending}
          className="w-full text-[10px]"
        >
          {isFull ? 'RACE FULL' : 'BOOK SLOT ▶'}
        </Button>
      )}
      {type === 'booked' && onCancel && (
        <Button
          variant="pink-ghost"
          onClick={onCancel}
          disabled={isPending}
          className="w-full text-[10px]"
        >
          ✕ CANCEL
        </Button>
      )}
    </div>
  )
}
```

**Step 2: Verify TypeScript**

```bash
npx tsc --noEmit 2>&1 | head -30
```
Expected: no errors.

**Step 3: Commit**

```bash
git add components/booking/TimeSlotCard.tsx
git commit -m "feat: redesign TimeSlotCard as Tron race entry pass"
```

---

## Task 4: Landing Page — Inline Auth + Perspective Grid

**Files:**
- Modify: `app/page.tsx` (full rewrite)

**Step 1: Rewrite app/page.tsx**

```tsx
// app/page.tsx
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/Button'
import { authService } from '@/services/authService'
import toast from 'react-hot-toast'

type AuthMode = 'login' | 'signup'

export default function HomePage() {
  const router = useRouter()
  const [mode, setMode] = useState<AuthMode>('login')
  const [isLoading, setIsLoading] = useState(false)
  const [form, setForm] = useState({ username: '', password: '', confirm: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const update = (field: string, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setErrors(prev => ({ ...prev, [field]: '' }))
  }

  const validate = (): boolean => {
    const e: Record<string, string> = {}
    if (!form.username.trim()) e.username = 'required'
    if (!form.password) e.password = 'required'
    else if (form.password.length < 5) e.password = 'min 5 chars'
    if (mode === 'signup' && form.password !== form.confirm) e.confirm = 'passwords differ'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setIsLoading(true)
    try {
      if (mode === 'login') {
        await authService.login(form.username, form.password)
        toast.success('ACCESS GRANTED')
      } else {
        await authService.register(form.username, `${form.username}@example.com`, form.password)
        toast.success('PLAYER REGISTERED')
      }
      router.push('/dashboard')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'AUTH FAILED')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden flex flex-col" style={{ background: 'var(--bg)' }}>

      {/* ── Hero content ── */}
      <div className="relative z-10 flex flex-col items-center justify-center flex-1 px-4 pb-[42%]">

        {/* Logo */}
        <div className="mb-8 text-center">
          <div className="font-['Orbitron'] font-black text-4xl sm:text-5xl text-neon-cyan" style={{ color: 'var(--cyan)' }}>
            ◈ APEX RACING
          </div>
          <div className="font-['DM_Mono'] text-xs tracking-[0.3em] mt-2" style={{ color: 'var(--dim)' }}>
            GO-KART BOOKING SYSTEM
          </div>
        </div>

        {/* Terminal card */}
        <div
          className="relative w-full max-w-sm p-6"
          style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
        >
          {/* Corner brackets */}
          <span className="bracket-corner tl" />
          <span className="bracket-corner tr" />
          <span className="bracket-corner bl" />
          <span className="bracket-corner br" />

          {/* Tab toggle */}
          <div className="flex mb-6 border-b" style={{ borderColor: 'var(--border)' }}>
            {(['login', 'signup'] as AuthMode[]).map(m => (
              <button
                key={m}
                onClick={() => { setMode(m); setErrors({}) }}
                className="flex-1 pb-2 font-['Orbitron'] text-[10px] tracking-widest uppercase transition-colors"
                style={{
                  color: mode === m ? 'var(--cyan)' : 'var(--dim)',
                  borderBottom: mode === m ? '1px solid var(--cyan)' : '1px solid transparent',
                  marginBottom: '-1px',
                }}
              >
                {m === 'login' ? 'SIGN IN' : 'NEW PLAYER'}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Username */}
            <div>
              <label className="font-['DM_Mono'] text-[10px] tracking-widest uppercase" style={{ color: 'var(--dim)' }}>
                USERNAME
              </label>
              <input
                className="tron-input mt-1"
                value={form.username}
                onChange={e => update('username', e.target.value)}
                placeholder="enter handle"
                autoComplete="username"
              />
              {errors.username && (
                <p className="font-['DM_Mono'] text-[10px] mt-1" style={{ color: 'var(--pink)' }}>
                  ✕ {errors.username}
                </p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="font-['DM_Mono'] text-[10px] tracking-widest uppercase" style={{ color: 'var(--dim)' }}>
                PASSWORD
              </label>
              <input
                className="tron-input mt-1"
                type="password"
                value={form.password}
                onChange={e => update('password', e.target.value)}
                placeholder="••••••••"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              />
              {errors.password && (
                <p className="font-['DM_Mono'] text-[10px] mt-1" style={{ color: 'var(--pink)' }}>
                  ✕ {errors.password}
                </p>
              )}
            </div>

            {/* Confirm password (signup only) */}
            {mode === 'signup' && (
              <div>
                <label className="font-['DM_Mono'] text-[10px] tracking-widest uppercase" style={{ color: 'var(--dim)' }}>
                  CONFIRM
                </label>
                <input
                  className="tron-input mt-1"
                  type="password"
                  value={form.confirm}
                  onChange={e => update('confirm', e.target.value)}
                  placeholder="••••••••"
                  autoComplete="new-password"
                />
                {errors.confirm && (
                  <p className="font-['DM_Mono'] text-[10px] mt-1" style={{ color: 'var(--pink)' }}>
                    ✕ {errors.confirm}
                  </p>
                )}
              </div>
            )}

            {/* Submit */}
            <Button
              type="submit"
              variant="cyan"
              disabled={isLoading}
              className="w-full mt-2 text-[10px]"
            >
              {isLoading ? '...' : mode === 'login' ? 'ENTER RACE ▶' : 'REGISTER PLAYER +'}
            </Button>
          </form>
        </div>
      </div>

      {/* ── Perspective grid floor ── */}
      <div className="grid-floor-container">
        <div className="grid-floor-inner" />
      </div>
    </div>
  )
}
```

**Step 2: Verify TypeScript**

```bash
npx tsc --noEmit 2>&1 | head -30
```
Expected: no errors. If AuthModal import remains elsewhere, you'll fix it in Task 6.

**Step 3: Commit**

```bash
git add app/page.tsx
git commit -m "feat: redesign landing page with inline auth and perspective grid floor"
```

---

## Task 5: Dashboard — Sidebar Layout + Date Strip

**Files:**
- Modify: `app/dashboard/page.tsx` (full rewrite)

**Step 1: Rewrite dashboard/page.tsx**

```tsx
// app/dashboard/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { LogOut, Zap, BookOpen } from 'lucide-react';
import { addDays, format } from 'date-fns';
import { TimeSlotCard } from '@/components/booking/TimeSlotCard';
import { bookingService } from '@/services/bookingService';
import { authService } from '@/services/authService';
import { useAuthStore } from '@/store/authStore';
import toast from 'react-hot-toast';

type Tab = 'available' | 'bookings';

function buildDateStrip() {
  const today = new Date();
  return Array.from({ length: 7 }, (_, i) => {
    const d = addDays(today, i);
    return {
      value: format(d, 'yyyy-MM-dd'),
      day: format(d, 'EEE').toUpperCase(),
      date: format(d, 'dd'),
    };
  });
}

const DATE_STRIP = buildDateStrip();

export default function DashboardPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isAuthenticated, user } = useAuthStore();
  const [activeTab, setActiveTab] = useState<Tab>('available');
  const [selectedDate, setSelectedDate] = useState(DATE_STRIP[0].value);

  useEffect(() => {
    if (!isAuthenticated) router.push('/');
  }, [isAuthenticated, router]);

  // SSE — push-invalidate on backend slot changes
  useEffect(() => {
    if (activeTab !== 'available') return;
    const API = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const es = new EventSource(`${API}/api/v1/slots/stream?date=${selectedDate}`);
    es.addEventListener('slot-update', () => {
      queryClient.invalidateQueries({ queryKey: ['slots', selectedDate] });
    });
    es.onerror = () => es.close();
    return () => es.close();
  }, [selectedDate, activeTab, queryClient]);

  const { data: slots = [], isLoading: slotsLoading, isFetching: slotsFetching } = useQuery({
    queryKey: ['slots', selectedDate],
    queryFn: () => bookingService.getSlots(selectedDate),
    refetchInterval: 30_000,
  });

  const { data: myBookings = [], isLoading: bookingsLoading } = useQuery({
    queryKey: ['my-bookings'],
    queryFn: () => bookingService.getMyBookings(),
  });

  const bookMutation = useMutation({
    mutationFn: ({ slotId, date }: { slotId: string; date: string }) =>
      bookingService.bookSlot(slotId, date, crypto.randomUUID()),
    onSuccess: () => {
      toast.success('SLOT BOOKED');
      queryClient.invalidateQueries({ queryKey: ['slots'] });
      queryClient.invalidateQueries({ queryKey: ['my-bookings'] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const cancelMutation = useMutation({
    mutationFn: (bookingId: string) => bookingService.cancelBooking(bookingId),
    onSuccess: () => {
      toast.success('BOOKING CANCELLED');
      queryClient.invalidateQueries({ queryKey: ['slots'] });
      queryClient.invalidateQueries({ queryKey: ['my-bookings'] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const handleLogout = async () => {
    await authService.logout();
    router.push('/');
  };

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center font-['Orbitron'] text-sm" style={{ background: 'var(--bg)', color: 'var(--dim)' }}>
        LOADING...
      </div>
    );
  }

  const availableSlots = slots.filter(s => s.available);

  const navItems: { tab: Tab; icon: React.ReactNode; label: string; count?: number }[] = [
    { tab: 'available', icon: <Zap size={14} />, label: 'SLOTS' },
    { tab: 'bookings', icon: <BookOpen size={14} />, label: 'BOOKINGS', count: myBookings.length },
  ];

  return (
    <div className="min-h-screen flex" style={{ background: 'var(--bg)' }}>

      {/* ── Sidebar (desktop) ── */}
      <aside
        className="hidden md:flex flex-col w-48 flex-shrink-0 border-r"
        style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}
      >
        {/* Logo */}
        <div className="px-4 py-5 border-b" style={{ borderColor: 'var(--border)' }}>
          <div className="font-['Orbitron'] font-black text-sm text-neon-cyan" style={{ color: 'var(--cyan)' }}>
            ◈ APEX
          </div>
          <div className="font-['DM_Mono'] text-[9px] tracking-widest mt-0.5" style={{ color: 'var(--dim)' }}>
            RACE CONTROL
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 py-4">
          {navItems.map(({ tab, icon, label, count }) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex items-center gap-2 w-full px-4 py-3 font-['Orbitron'] text-[10px] tracking-widest transition-all ${
                activeTab === tab ? 'nav-item-active' : 'nav-item-inactive'
              }`}
            >
              {icon}
              {label}
              {count !== undefined && count > 0 && (
                <span
                  className="ml-auto text-[9px] px-1.5 py-0.5 rounded-sm"
                  style={{ background: 'rgba(0,207,255,0.15)', color: 'var(--cyan)' }}
                >
                  {count}
                </span>
              )}
            </button>
          ))}
        </nav>

        {/* User + logout */}
        <div className="p-4 border-t" style={{ borderColor: 'var(--border)' }}>
          <p className="font-['DM_Mono'] text-[10px] truncate mb-2" style={{ color: 'var(--dim)' }}>
            {user?.username}
          </p>
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 font-['Orbitron'] text-[9px] tracking-widest transition-colors"
            style={{ color: 'var(--dim)' }}
            onMouseEnter={e => (e.currentTarget.style.color = 'var(--pink)')}
            onMouseLeave={e => (e.currentTarget.style.color = 'var(--dim)')}
          >
            <LogOut size={12} />
            EJECT
          </button>
        </div>
      </aside>

      {/* ── Main content ── */}
      <div className="flex-1 flex flex-col min-w-0 pb-16 md:pb-0">

        {/* Header bar */}
        <header
          className="flex items-center justify-between px-4 md:px-6 py-4 border-b flex-shrink-0"
          style={{ borderColor: 'var(--border)' }}
        >
          <div className="flex items-center gap-3">
            <h1 className="font-['Orbitron'] font-bold text-lg" style={{ color: 'var(--white)' }}>
              RACE CONTROL
            </h1>
            {slotsFetching && <span className="fetch-dot" />}
          </div>
          {/* Mobile user */}
          <span className="md:hidden font-['DM_Mono'] text-xs" style={{ color: 'var(--dim)' }}>
            {user?.username}
          </span>
        </header>

        <div className="flex-1 px-4 md:px-6 py-5 overflow-auto">

          {/* Date strip (available tab only) */}
          {activeTab === 'available' && (
            <div className="flex gap-2 overflow-x-auto pb-2 mb-5 scrollbar-hide">
              {DATE_STRIP.map(({ value, day, date }) => (
                <button
                  key={value}
                  onClick={() => setSelectedDate(value)}
                  className="flex-shrink-0 flex flex-col items-center px-3 py-2 rounded-none border transition-all font-['Orbitron'] text-[10px] tracking-wider"
                  style={
                    selectedDate === value
                      ? {
                          borderColor: 'var(--cyan)',
                          background: 'rgba(0,207,255,0.1)',
                          color: 'var(--cyan)',
                          boxShadow: '0 0 10px rgba(0,207,255,0.2)',
                        }
                      : {
                          borderColor: 'var(--border)',
                          background: 'transparent',
                          color: 'var(--dim)',
                        }
                  }
                >
                  <span className="text-[8px]">{day}</span>
                  <span className="text-base font-black leading-tight">{date}</span>
                </button>
              ))}
            </div>
          )}

          {/* Content grid */}
          {(slotsLoading || bookingsLoading) ? (
            <div className="flex justify-center py-16">
              <div
                className="w-8 h-8 rounded-full border-t border-r animate-spin"
                style={{ borderColor: 'var(--cyan)' }}
              />
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                {activeTab === 'available' && availableSlots.map(slot => (
                  <TimeSlotCard
                    key={slot.id}
                    timeSlot={slot}
                    onBook={() => bookMutation.mutate({ slotId: slot.id, date: selectedDate })}
                    type="available"
                    isPending={bookMutation.isPending}
                  />
                ))}

                {activeTab === 'bookings' && myBookings.map(booking => (
                  // Construct a minimal TimeSlot shape for the booked card
                  <TimeSlotCard
                    key={booking.id}
                    timeSlot={{
                      id: booking.timeSlotId,
                      startTime: booking.startTime ?? '—',
                      endTime: booking.endTime ?? '—',
                      capacity: 10,
                      remaining: 0,
                      available: false,
                    }}
                    onCancel={() => cancelMutation.mutate(booking.id)}
                    type="booked"
                    isPending={cancelMutation.isPending}
                  />
                ))}
              </div>

              {/* Empty states */}
              {activeTab === 'available' && availableSlots.length === 0 && (
                <div className="flex flex-col items-center justify-center py-20 gap-3">
                  <div
                    className="font-['Orbitron'] font-black text-2xl tracking-widest"
                    style={{ color: 'var(--dim)' }}
                  >
                    NO RACES SCHEDULED
                  </div>
                  <div className="font-['DM_Mono'] text-xs" style={{ color: 'var(--dim)' }}>
                    select another date
                  </div>
                </div>
              )}

              {activeTab === 'bookings' && myBookings.length === 0 && (
                <div className="flex flex-col items-center justify-center py-20 gap-3">
                  <div
                    className="font-['Orbitron'] font-black text-2xl tracking-widest"
                    style={{ color: 'var(--dim)' }}
                  >
                    NO ACTIVE BOOKINGS
                  </div>
                  <div className="font-['DM_Mono'] text-xs" style={{ color: 'var(--dim)' }}>
                    head to slots to book a race
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* ── Bottom tab bar (mobile only) ── */}
      <nav
        className="md:hidden fixed bottom-0 left-0 right-0 flex border-t"
        style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}
      >
        {navItems.map(({ tab, icon, label }) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className="flex-1 flex flex-col items-center py-3 gap-1 font-['Orbitron'] text-[9px] tracking-widest transition-colors"
            style={{ color: activeTab === tab ? 'var(--cyan)' : 'var(--dim)' }}
          >
            {icon}
            {label}
          </button>
        ))}
        <button
          onClick={handleLogout}
          className="flex-1 flex flex-col items-center py-3 gap-1 font-['Orbitron'] text-[9px] tracking-widest"
          style={{ color: 'var(--dim)' }}
        >
          <LogOut size={14} />
          EJECT
        </button>
      </nav>
    </div>
  );
}
```

> **Note on booking data shape:** If `bookingService.getMyBookings()` returns bookings without `startTime`/`endTime`, the booked cards will show `—`. Check `services/bookingService.ts` — if the Booking type lacks those fields, either add them to the API response or display `booking.bookingDate` + `booking.timeSlotId.slice(0,8)` instead. Adjust the TimeSlot shape construction accordingly.

**Step 2: Verify TypeScript**

```bash
npx tsc --noEmit 2>&1 | head -40
```

Fix any type errors. The most likely one: `myBookings` items may not have `startTime`/`endTime`. If so, replace with:
```tsx
timeSlot={{
  id: booking.timeSlotId,
  startTime: booking.bookingDate,
  endTime: '',
  capacity: 1,
  remaining: 0,
  available: false,
}}
```

**Step 3: Commit**

```bash
git add app/dashboard/page.tsx
git commit -m "feat: redesign dashboard with sidebar, date strip, Tron layout"
```

---

## Task 6: Cleanup — Remove AuthModal

**Files:**
- Delete: `components/auth/AuthModal.tsx`

**Step 1: Confirm no remaining imports**

```bash
grep -r "AuthModal" "/Users/krish/Library/Mobile Documents/com~apple~CloudDocs/Projects/Go Karting Booking System" --include="*.tsx" --include="*.ts"
```
Expected: no results (we already removed its import from page.tsx in Task 4).

**Step 2: Delete the file**

```bash
rm "/Users/krish/Library/Mobile Documents/com~apple~CloudDocs/Projects/Go Karting Booking System/components/auth/AuthModal.tsx"
```

**Step 3: Verify build still passes**

```bash
npx tsc --noEmit 2>&1 | head -20
```
Expected: no errors.

**Step 4: Commit**

```bash
git add -A
git commit -m "chore: remove AuthModal — auth now inline on landing page"
```

---

## Task 7: Font Loading in layout.tsx

**Files:**
- Modify: `app/layout.tsx`

**Step 1: Read current layout.tsx**

Open and read the file to see its exact structure (it may use `next/font` or a plain `<head>`).

**Step 2: Add Google Fonts link**

In App Router, add a `<link>` inside the `<head>` export. If the layout uses the `metadata` export, add a separate exported `viewport` and insert links in the JSX `<head>`:

```tsx
// If layout.tsx has a plain <html><head>...</head><body> structure, add inside <head>:
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
<link
  href="https://fonts.googleapis.com/css2?family=Orbitron:wght@700;900&family=DM+Mono:wght@400;500&display=swap"
  rel="stylesheet"
/>
```

If layout.tsx does NOT have an explicit `<head>`, wrap the font links in Next.js's built-in `<Script>` or use the approach below with `next/font/google`:

```tsx
// Alternative: next/font/google (no <link> needed, fully optimized)
import { Orbitron, DM_Mono } from 'next/font/google'

const orbitron = Orbitron({ subsets: ['latin'], weight: ['700', '900'], variable: '--font-orbitron' })
const dmMono = DM_Mono({ subsets: ['latin'], weight: ['400', '500'], variable: '--font-dm-mono' })

// Then on <html> element:
<html className={`${orbitron.variable} ${dmMono.variable}`}>
```

With `next/font`, update CSS references from `font-family: 'Orbitron'` to `font-family: var(--font-orbitron)` and similarly for DM Mono. This is the preferred Next.js approach.

**Step 3: Verify fonts load**

```bash
npm run dev
```
Open http://localhost:3000, open DevTools → Network → filter "fonts" — confirm Orbitron and DM Mono are loaded.

**Step 4: Commit**

```bash
git add app/layout.tsx
git commit -m "feat: load Orbitron and DM Mono fonts via next/font"
```

---

## Task 8: End-to-End Smoke Test

**Step 1: Start the full stack**

```bash
# Terminal 1 — backend + infra
docker compose up

# Terminal 2 — frontend
npm run dev
```

**Step 2: Smoke test checklist**

1. Open http://localhost:3000 — verify:
   - Deep navy-black background, scanline overlay visible
   - Perspective grid floor animating at bottom
   - "◈ APEX RACING" in Orbitron with cyan glow
   - Terminal card with SIGN IN / NEW PLAYER tabs

2. Register a new user — verify:
   - Toast "PLAYER REGISTERED" appears
   - Redirects to /dashboard

3. On dashboard — verify:
   - Sidebar visible on desktop with SLOTS / BOOKINGS nav
   - Bottom tab bar on mobile (resize window)
   - Date strip showing 7 days, today selected
   - TimeSlotCards render with dashed borders and Orbitron times

4. Book a slot — verify:
   - "SLOT BOOKED" toast
   - Card appears under BOOKINGS tab with pink border

5. Cancel a booking — verify:
   - "BOOKING CANCELLED" toast
   - Card removed

6. Logout — verify redirect to landing page

**Step 3: Fix any visual issues found**

Common fixes:
- Font not applying: check `font-family` CSS vs CSS variable name
- Grid floor not visible: check `.grid-floor-container` height and `perspective` value
- Cards misaligned: check `position: relative` on card wrapper for corner brackets

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete Tron frontend redesign — Race Control aesthetic"
```
