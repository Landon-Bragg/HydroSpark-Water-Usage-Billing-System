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
// frontend/src/services/customerService.ts
  async getUsageHistory(customerId: string): Promise<UsageDataPoint[]> {
    // REPLACE the fake forecast-based data with:
    const res = await api.get(`/api/meter-readings/customer/${customerId}`, {
      params: {
        startDate: thirtyDaysAgo,
        endDate: today,
      }
    });
    return res.data.map(reading => ({
      date: reading.readingDate,
      usageCcf: Number(reading.usageCcf)
    }));
  }

  async getCurrentBill(customerId: string): Promise<BillSummary> {
    // REPLACE with actual bills endpoint:
    const res = await api.get(`/api/customers/${customerId}/bills`);
    const bills = res.data;
    const latest = bills[0]; // assuming sorted by date desc
    
    return {
      currentBill: latest ? Number(latest.totalAmount) : 0,
      estimatedNextBill: estimatedAmount, // from forecast
      dueDate: latest?.dueDate || '...'
    };
  },
};
