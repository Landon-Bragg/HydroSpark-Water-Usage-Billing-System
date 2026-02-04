import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authService } from '../services/authService';

export type UserRole = 'ADMIN' | 'BILLING' | 'OPERATIONS' | 'SUPPORT' | 'CUSTOMER';

export interface AuthUser {
  userId: string;
  email: string;
  role: UserRole;
  customerId?: string | null;
}

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

const LS_KEYS = {
  accessToken: 'hydrospark.accessToken',
  refreshToken: 'hydrospark.refreshToken',
  user: 'hydrospark.user',
};

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  // boot
  useEffect(() => {
    const boot = async () => {
      try {
        const storedUser = localStorage.getItem(LS_KEYS.user);
        const storedAccess = localStorage.getItem(LS_KEYS.accessToken);
        const storedRefresh = localStorage.getItem(LS_KEYS.refreshToken);

        if (storedUser) setUser(JSON.parse(storedUser));
        if (storedAccess) setAccessToken(storedAccess);
        if (storedRefresh) setRefreshToken(storedRefresh);

        // If we have a refresh token but no access token (or an expired one),
        // attempt a refresh once on boot.
        if (storedRefresh && !storedAccess) {
          const res = await authService.refresh(storedRefresh);
          persistSession(res.accessToken, res.refreshToken, {
            userId: res.userId,
            email: res.email,
            role: res.role as any,
            customerId: res.customerId,
          });
        }
      } catch (e) {
        // If boot fails, clear local storage
        clearSession();
      } finally {
        setLoading(false);
      }
    };

    boot();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const persistSession = (newAccess: string, newRefresh: string, newUser: AuthUser) => {
    setAccessToken(newAccess);
    setRefreshToken(newRefresh);
    setUser(newUser);
    localStorage.setItem(LS_KEYS.accessToken, newAccess);
    localStorage.setItem(LS_KEYS.refreshToken, newRefresh);
    localStorage.setItem(LS_KEYS.user, JSON.stringify(newUser));
  };

  const clearSession = () => {
    setAccessToken(null);
    setRefreshToken(null);
    setUser(null);
    localStorage.removeItem(LS_KEYS.accessToken);
    localStorage.removeItem(LS_KEYS.refreshToken);
    localStorage.removeItem(LS_KEYS.user);
  };

  const login = async (email: string, password: string) => {
    const res = await authService.login(email, password);
    persistSession(res.accessToken, res.refreshToken, {
      userId: res.userId,
      email: res.email,
      role: res.role as any,
      customerId: res.customerId,
    });
  };

  const logout = () => {
    // best-effort backend logout; ignore errors
    authService.logout().catch(() => {});
    clearSession();
  };

  const value = useMemo<AuthState>(() => ({
    user,
    accessToken,
    refreshToken,
    loading,
    login,
    logout,
  }), [user, accessToken, refreshToken, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

export const authStorageKeys = LS_KEYS;
