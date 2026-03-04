import { useAuthStore } from '@/store/authStore';

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export interface TimeSlot {
  id: string;
  startTime: string;
  endTime: string;
  capacity: number;
  remaining: number;
  available: boolean;
}

export interface Booking {
  id: string;
  timeSlotId: string;
  startTime: string;
  endTime: string;
  bookingDate: string;
  status: string;
  createdAt: string;
  bookedBy: string;
  racerCount: number;
  racerNames: string[];
}

async function authFetch(path: string, init?: RequestInit): Promise<Response> {
  const token = useAuthStore.getState().accessToken;
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (res.status === 401) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      const newToken = useAuthStore.getState().accessToken;
      headers.set('Authorization', `Bearer ${newToken}`);
      return fetch(`${API_BASE}${path}`, { ...init, headers });
    }
    useAuthStore.getState().clearAuth();
    if (typeof window !== 'undefined') window.location.href = '/';
  }

  return res;
}

async function tryRefresh(): Promise<boolean> {
  const { refreshToken, setTokens, clearAuth } = useAuthStore.getState();
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) { clearAuth(); return false; }

    const data = await res.json();
    setTokens(data.accessToken, data.refreshToken, { username: data.username, role: data.role });
    return true;
  } catch {
    clearAuth();
    return false;
  }
}

export const bookingService = {
  getSlots: async (date?: string): Promise<TimeSlot[]> => {
    const url = date ? `/api/v1/slots?date=${date}` : '/api/v1/slots';
    const res = await authFetch(url);
    if (!res.ok) throw new Error('Failed to fetch time slots');
    return res.json();
  },

  getMyBookings: async (): Promise<Booking[]> => {
    const res = await authFetch('/api/v1/bookings');
    if (!res.ok) throw new Error('Failed to fetch bookings');
    return res.json();
  },

  bookSlot: async (
    timeSlotId: string,
    bookingDate: string,
    racerCount: number,
    racerNames: string[],
    idempotencyKey?: string,
  ): Promise<Booking> => {
    const extraHeaders: HeadersInit = idempotencyKey
      ? { 'X-Idempotency-Key': idempotencyKey }
      : {};

    const res = await authFetch('/api/v1/bookings', {
      method: 'POST',
      body: JSON.stringify({ timeSlotId, bookingDate, racerCount, racerNames }),
      headers: extraHeaders,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error((err as any).detail ?? 'Failed to book slot');
    }
    return res.json();
  },

  cancelBooking: async (bookingId: string): Promise<void> => {
    const res = await authFetch(`/api/v1/bookings/${bookingId}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error((err as any).detail ?? 'Failed to cancel booking');
    }
  },
};
