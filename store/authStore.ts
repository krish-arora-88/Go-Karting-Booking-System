import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthUser {
  username: string;
  role: string;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  hasHydrated: boolean;

  setTokens: (accessToken: string, refreshToken: string, user: AuthUser) => void;
  clearAuth: () => void;
  updateAccessToken: (accessToken: string) => void;
  setHasHydrated: (v: boolean) => void;
}

/**
 * Zustand auth store with localStorage persistence.
 * Centralises all auth state — no direct localStorage calls in components.
 * hasHydrated gates auth-dependent redirects until persist middleware has
 * finished reading from localStorage (prevents refresh-logout race condition).
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      hasHydrated: false,

      setTokens: (accessToken, refreshToken, user) =>
        set({ accessToken, refreshToken, user, isAuthenticated: true }),

      clearAuth: () =>
        set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false }),

      updateAccessToken: (accessToken) =>
        set({ accessToken }),

      setHasHydrated: (v) => set({ hasHydrated: v }),
    }),
    {
      name: 'gokarting-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    }
  )
);
