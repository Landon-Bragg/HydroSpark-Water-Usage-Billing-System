import { api } from './api';

// Import existing types from your current staffService.ts
// This file extends it with billing period management

export interface BillingPeriod {
  id: string;
  cycleNumber: number;
  periodStartDate: string;
  periodEndDate: string;
  status: string; // OPEN, CLOSED, BILLED
  createdAt: string;
}

export interface BillingRunResult {
  periodId: string;
  totalCustomers: number;
  successCount: number;
  failedCount: number;
  totalAmount: string;
  completedAt: string;
  errors: BillingError[];
}

export interface BillingError {
  customerId: string;
  message: string;
}

export interface SendBillsResult {
  totalBills: number;
  sentCount: number;
  failedCount: number;
}

export const billingPeriodService = {
  /**
   * Get all billing periods
   */
  async getAllPeriods(): Promise<BillingPeriod[]> {
    const res = await api.get<BillingPeriod[]>('/api/billing/periods');
    return res.data;
  },

  /**
   * Get billing periods for a specific cycle
   */
  async getPeriodsByCycle(cycleNumber: number): Promise<BillingPeriod[]> {
    const res = await api.get<BillingPeriod[]>(`/api/billing/periods/cycle/${cycleNumber}`);
    return res.data;
  },

  /**
   * Get a specific billing period
   */
  async getPeriod(periodId: string): Promise<BillingPeriod> {
    const res = await api.get<BillingPeriod>(`/api/billing/periods/${periodId}`);
    return res.data;
  },

  /**
   * Generate a new billing period
   */
  async generatePeriod(
    cycleNumber: number,
    periodStart: string,
    periodEnd: string
  ): Promise<BillingPeriod> {
    const res = await api.post<BillingPeriod>('/api/billing/periods/generate', {
      cycleNumber,
      periodStart,
      periodEnd,
    });
    return res.data;
  },

  /**
   * Generate monthly period for current month
   */
  async generateMonthlyPeriod(cycleNumber: number): Promise<BillingPeriod> {
    const res = await api.post<BillingPeriod>(
      `/api/billing/periods/generate-monthly/${cycleNumber}`
    );
    return res.data;
  },

  /**
   * Run billing for a period (generate all bills)
   */
  async runBilling(periodId: string): Promise<BillingRunResult> {
    const res = await api.post<BillingRunResult>(`/api/billing/periods/${periodId}/run`);
    return res.data;
  },

  /**
   * Issue bills for a period
   */
  async issueBills(periodId: string): Promise<void> {
    await api.post(`/api/billing/periods/${periodId}/issue`);
  },

  /**
   * Send bills via email for a period
   */
  async sendBills(periodId: string): Promise<SendBillsResult> {
    const res = await api.post<SendBillsResult>(`/api/billing/periods/${periodId}/send`);
    return res.data;
  },

  /**
   * Get bills for a period
   */
  async getBillsForPeriod(periodId: string): Promise<any[]> {
    const res = await api.get(`/api/bills/period/${periodId}`);
    return res.data;
  },
};
