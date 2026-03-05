import { useAuthStore } from '@/store/authStore';

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

function fallbackMessage(status: number, context: 'login' | 'register'): string {
  switch (status) {
    case 400: return 'Invalid input — check your fields';
    case 401: return 'Invalid username or password';
    case 403: return 'Access denied';
    case 409: return context === 'register' ? 'Username already taken' : 'Conflict';
    case 429: return 'Too many attempts — try again later';
    default:  return status >= 500 ? 'Server error — try again shortly' : 'Something went wrong';
  }
}

async function parseError(res: Response, context: 'login' | 'register'): Promise<string> {
  const body = await res.json().catch(() => null);
  if (body?.detail) return body.detail;
  return fallbackMessage(res.status, context);
}

export const authService = {
  login: async (username: string, password: string) => {
    const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    if (!res.ok) throw new Error(await parseError(res, 'login'));

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

    if (!res.ok) throw new Error(await parseError(res, 'register'));

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
