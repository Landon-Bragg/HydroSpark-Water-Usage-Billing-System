import { api } from './api';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  role: string;       // ADMIN/BILLING/OPERATIONS/SUPPORT/CUSTOMER
  customerId?: string | null;
}

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const res = await api.post<LoginResponse>('/api/auth/login', { email, password });
    return res.data;
  },

  async refresh(refreshToken: string): Promise<LoginResponse> {
    const res = await api.post<LoginResponse>('/api/auth/refresh', { refreshToken });
    return res.data;
  },

  async logout(): Promise<void> {
    await api.post('/api/auth/logout');
  },

  async resetPassword(email: string): Promise<void> {
    await api.post('/api/auth/reset-password', { email });
  },

  async changePassword(userId: string, oldPassword: string, newPassword: string): Promise<void> {
    await api.post('/api/auth/change-password', { userId, oldPassword, newPassword });
  }
};
