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

export interface MeterReadingDTO {
  id: string;
  meterId: string;
  readingDate: string; // ISO date
  usageCcf: string;    // BigDecimal as string
  source: string;
  ingestedAt: string;
}

export interface UsageDataPoint {
  date: string;
  usageCcf: number;
}

export interface BillDTO {
  id: string;
  customerId: string;
  meterId: string;
  billingPeriodId: string;
  issueDate: string;
  dueDate: string;
  status: string;
  subtotal: string;
  totalFees: string;
  totalSurcharges: string;
  totalAmount: string;
  deliveredVia?: string;
  lineItems: LineItemDTO[];
}

export interface LineItemDTO {
  id: string;
  description: string;
  amount: string;
  category: string;
}

export interface BillSummary {
  currentBill: number;
  estimatedNextBill: number;
  dueDate: string;
}

export const customerService = {
  /**
   * Get customer statistics
   */
  async getCustomerStats(customerId: string): Promise<CustomerStats> {
    const res = await api.get<CustomerStats>(`/api/customers/${customerId}/stats`);
    return res.data;
  },

  /**
   * Get latest usage forecast
   */
  async getLatestForecast(customerId: string): Promise<UsageForecast | null> {
    try {
      const res = await api.get<UsageForecast>(`/api/forecast/${customerId}/latest`);
      return res.data;
    } catch (e: any) {
      // Forecast may not exist yet
      return null;
    }
  },

  /**
   * Generate new forecast
   */
  async generateForecast(customerId: string): Promise<UsageForecast> {
    const res = await api.post<UsageForecast>(`/api/forecast/${customerId}/generate`);
    return res.data;
  },

  /**
   * Get actual usage history from meter readings
   */
  async getUsageHistory(customerId: string, days: number = 30): Promise<UsageDataPoint[]> {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    const formatDate = (d: Date) => d.toISOString().split('T')[0];

    try {
      const res = await api.get<MeterReadingDTO[]>(`/api/meter-readings/customer/${customerId}`, {
        params: {
          startDate: formatDate(startDate),
          endDate: formatDate(endDate),
        },
      });

      // Convert to chart data points
      return res.data.map(reading => ({
        date: reading.readingDate,
        usageCcf: Number(reading.usageCcf),
      }));
    } catch (e) {
      console.error('Error fetching usage history:', e);
      return [];
    }
  },

  /**
   * Get current and estimated bill information
   */
  async getCurrentBill(customerId: string): Promise<BillSummary> {
    try {
      // Get current unpaid bill
      const currentRes = await api.get<BillDTO>(`/api/bills/customer/${customerId}/current`);
      const currentBill = currentRes.data;

      // Get forecast for next bill
      const forecast = await this.getLatestForecast(customerId);
      const estimatedNextBill = forecast ? Number(forecast.predictedTotalAmount) : 0;

      return {
        currentBill: currentBill ? Number(currentBill.totalAmount) : 0,
        estimatedNextBill: Number.isFinite(estimatedNextBill) ? estimatedNextBill : 0,
        dueDate: currentBill?.dueDate || (forecast?.targetPeriodEnd || '—'),
      };
    } catch (e: any) {
      // If no current bill, just use forecast
      const forecast = await this.getLatestForecast(customerId);
      const est = forecast ? Number(forecast.predictedTotalAmount) : 0;

      return {
        currentBill: 0,
        estimatedNextBill: Number.isFinite(est) ? est : 0,
        dueDate: forecast?.targetPeriodEnd || '—',
      };
    }
  },

  /**
   * Get all bills for a customer
   */
  async getAllBills(customerId: string): Promise<BillDTO[]> {
    const res = await api.get<BillDTO[]>(`/api/bills/customer/${customerId}`);
    return res.data;
  },

  /**
   * Get bill details
   */
  async getBillDetails(billId: string): Promise<BillDTO> {
    const res = await api.get<BillDTO>(`/api/bills/${billId}`);
    return res.data;
  },

  /**
   * Record payment for a bill
   */
  async payBill(billId: string, amount: number, paymentMethod: string): Promise<BillDTO> {
    const res = await api.post<BillDTO>(`/api/bills/${billId}/pay`, {
      amount: amount.toString(),
      paymentMethod,
      transactionId: `TXN-${Date.now()}`,
    });
    return res.data;
  },
};
