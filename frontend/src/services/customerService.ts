import { api } from './api';

export interface CustomerStats {
  customerId: string;
  customerName: string;
  totalMeters: number;
  activeMeters: number;
}

export interface UsageForecast {
  id: string;
  customerId: string;
  meterId: string;
  billingCycleNumber: number;
  targetPeriodStart: string; // ISO date
  targetPeriodEnd: string;   // ISO date
  predictedTotalCcf: string; // BigDecimal serialized as string
  predictedTotalAmount: string;
  method: string;
  confidenceLevel: string;
  historicalPeriodsUsed: number;
  generatedAt: string;
}

export interface UsageDataPoint {
  date: string;
  usageCcf: number;
}

export interface BillSummary {
  // Your backend doesn't expose a bill endpoint yet; we synthesize from forecast.
  currentBill: number;
  estimatedNextBill: number;
  dueDate: string;
}

function daysBetween(start: Date, end: Date): number {
  const ms = 24 * 60 * 60 * 1000;
  return Math.max(1, Math.round((end.getTime() - start.getTime()) / ms) + 1);
}

export const customerService = {
  async getCustomerStats(customerId: string): Promise<CustomerStats> {
    const res = await api.get<CustomerStats>(`/api/customers/${customerId}/stats`);
    return res.data;
  },

  async getLatestForecast(customerId: string): Promise<UsageForecast | null> {
    try {
      const res = await api.get<UsageForecast>(`/api/forecast/${customerId}/latest`);
      return res.data;
    } catch (e: any) {
      // ForecastService may throw if none exists.
      return null;
    }
  },

  async generateForecast(customerId: string): Promise<UsageForecast> {
    const res = await api.post<UsageForecast>(`/api/forecast/${customerId}/generate`);
    return res.data;
  },

  /**
   * The backend doesn't have a "usage history" endpoint yet.
   * For plug-and-play UI, we generate an estimated daily series from the latest forecast.
   */
  async getUsageHistory(customerId: string): Promise<UsageDataPoint[]> {
    const forecast = await this.getLatestForecast(customerId);
    if (!forecast) return [];

    const start = new Date(forecast.targetPeriodStart);
    const end = new Date(forecast.targetPeriodEnd);
    const nDays = daysBetween(start, end);

    const total = Number(forecast.predictedTotalCcf);
    const perDay = Number.isFinite(total) ? (total / nDays) : 0;

    const points: UsageDataPoint[] = [];
    for (let i = 0; i < nDays; i++) {
      const d = new Date(start.getTime());
      d.setDate(d.getDate() + i);
      const iso = d.toISOString().slice(0, 10);
      points.push({ date: iso, usageCcf: Math.max(0, perDay) });
    }

    // If the billing period is longer than 30 days, show last 30.
    return points.slice(-30);
  },

  async getCurrentBill(customerId: string): Promise<BillSummary> {
    // No bills endpoint yet -> map from forecast.
    const forecast = await this.getLatestForecast(customerId);
    const est = forecast ? Number(forecast.predictedTotalAmount) : 0;

    // naive due date: end of forecast period (or today+14)
    const due = forecast?.targetPeriodEnd ?? new Date(Date.now() + 14*24*60*60*1000).toISOString().slice(0,10);

    return {
      currentBill: 0,
      estimatedNextBill: Number.isFinite(est) ? est : 0,
      dueDate: due,
    };
  },
};
