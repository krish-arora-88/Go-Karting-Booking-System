import { useAuthStore } from '@/store/authStore';

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export const authService = {
  login: async (username: string, password: string) => {
    const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error((err as any).detail ?? 'Invalid credentials');
    }

    const data = await res.json();
    useAuthStore.getState().setTokens(data.accessToken, data.refreshToken, {
      username: data.username,
      role: data.role,
    });
    return data;
  },

  register: async (username: string, email: string, password: string) => {
    const res = await fetch(`${API_BASE}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error((err as any).detail ?? 'Registration failed');
    }

    const data = await res.json();
    useAuthStore.getState().setTokens(data.accessToken, data.refreshToken, {
      username: data.username,
      role: data.role,
    });
    return data;
  },

  logout: async () => {
    const { accessToken, refreshToken } = useAuthStore.getState();
    try {
      await fetch(`${API_BASE}/api/v1/auth/logout`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        },
        body: JSON.stringify({ refreshToken }),
      });
    } finally {
      useAuthStore.getState().clearAuth();
    }
  },
};
