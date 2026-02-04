import axios from 'axios';
import { authStorageKeys } from '../contexts/AuthContext';

// Supports both CRA (process.env.REACT_APP_*) and Vite (import.meta.env.VITE_*)
function getBaseUrl(): string {
  // @ts-ignore
  const vite = (import.meta as any)?.env?.VITE_API_BASE_URL;
  const cra = (process as any)?.env?.REACT_APP_API_BASE_URL;
  // If proxy is configured, empty baseURL works fine.
  return (vite || cra || '').toString().replace(/\/$/, '');
}

export const api = axios.create({
  baseURL: getBaseUrl(),
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(authStorageKeys.accessToken);
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Single-flight refresh on 401
let isRefreshing = false;
let refreshQueue: Array<(token: string | null) => void> = [];

function queueRefresh(cb: (token: string | null) => void) {
  refreshQueue.push(cb);
}
function flushQueue(token: string | null) {
  refreshQueue.forEach(cb => cb(token));
  refreshQueue = [];
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;

    if (status !== 401 || original?._retry) {
      return Promise.reject(error);
    }

    const refreshToken = localStorage.getItem(authStorageKeys.refreshToken);
    if (!refreshToken) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        queueRefresh((token) => {
          if (!token) return reject(error);
          original.headers.Authorization = `Bearer ${token}`;
          resolve(api(original));
        });
      });
    }

    original._retry = true;
    isRefreshing = true;

    try {
      const { authService } = await import('./authService');
      const refreshed = await authService.refresh(refreshToken);

      localStorage.setItem(authStorageKeys.accessToken, refreshed.accessToken);
      // refresh token stays the same, but if backend rotates it, store it:
      if (refreshed.refreshToken) {
        localStorage.setItem(authStorageKeys.refreshToken, refreshed.refreshToken);
      }

      flushQueue(refreshed.accessToken);

      original.headers.Authorization = `Bearer ${refreshed.accessToken}`;
      return api(original);
    } catch (e) {
      flushQueue(null);
      return Promise.reject(e);
    } finally {
      isRefreshing = false;
    }
  }
);
