import { api } from './api';

export interface CustomerDTO {
  id?: string;
  name: string;
  customerType?: string;
  phone?: string;
  email?: string;
  mailingAddress?: string;
  mailingCity?: string;
  mailingState?: string;
  mailingZip?: string;
  billingCycleNumber?: number;
}

export interface RateComponentDTO {
  id?: string;
  ratePlanId?: string;
  componentType?: string;
  name?: string;
  configJson?: string;
  sortOrder?: number;
  isActive?: boolean;
}

export interface RatePlanDTO {
  id?: string;
  name?: string;
  customerTypeScope?: string;
  effectiveStartDate?: string;
  effectiveEndDate?: string | null;
  status?: string;
  components?: RateComponentDTO[];
}

export interface AnomalyEventDTO {
  id: string;
  customerId: string;
  meterId: string;
  eventDate: string;
  eventType: string;
  severity: string;
  description: string;
  status: string;
  createdBy?: string | null;
  resolvedBy?: string | null;
  resolutionNote?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ImportRun {
  id: string;
  filename: string;
  status: string;
  startedAt: string;
  completedAt?: string | null;
  totalRows?: number | null;
  successRows?: number | null;
  errorRows?: number | null;
  initiatedByUserId?: string | null;
}

export interface ChargeLine {
  description: string;
  amount: string; // BigDecimal => string
  category: string;
}

export interface ChargeBreakdown {
  subtotal: string;
  totalFees: string;
  totalSurcharges: string;
  totalAmount: string;
  lineItems: ChargeLine[];
}

export const staffService = {
  async listCustomers(page = 0, size = 25) {
    // Spring pageable format: ?page=0&size=25&sort=...
    const res = await api.get(`/api/customers?page=${page}&size=${size}`);
    return res.data; // Spring Page<CustomerDTO>
  },

  async searchCustomers(q: string): Promise<CustomerDTO[]> {
    const res = await api.get<CustomerDTO[]>(`/api/customers/search?q=${encodeURIComponent(q)}`);
    return res.data;
  },

  async listRatePlans(status?: string): Promise<RatePlanDTO[]> {
    const url = status ? `/api/rates/plans?status=${encodeURIComponent(status)}` : '/api/rates/plans';
    const res = await api.get<RatePlanDTO[]>(url);
    return res.data;
  },

  async createRatePlan(dto: RatePlanDTO): Promise<RatePlanDTO> {
    const res = await api.post<RatePlanDTO>('/api/rates/plans', dto);
    return res.data;
  },

  async updateRatePlan(planId: string, dto: RatePlanDTO): Promise<RatePlanDTO> {
    const res = await api.put<RatePlanDTO>(`/api/rates/plans/${planId}`, dto);
    return res.data;
  },

  async deleteRatePlan(planId: string): Promise<void> {
    await api.delete(`/api/rates/plans/${planId}`);
  },

  async calculateCharges(ratePlanId: string, usageCcf: string, billingDate?: string): Promise<ChargeBreakdown> {
    const payload: any = { ratePlanId, usageCcf };
    if (billingDate) payload.billingDate = billingDate;
    const res = await api.post<ChargeBreakdown>('/api/rates/calculate', payload);
    return res.data;
  },

  async listAnomalies(status?: string, severity?: string): Promise<AnomalyEventDTO[]> {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (severity) params.set('severity', severity);
    const qs = params.toString();
    const res = await api.get<AnomalyEventDTO[]>(`/api/anomalies${qs ? `?${qs}` : ''}`);
    return res.data;
  },

  async runAnomalyDetection(meterId?: string) {
    const url = meterId ? `/api/anomalies/run?meterId=${encodeURIComponent(meterId)}` : '/api/anomalies/run';
    const res = await api.post(url);
    return res.data;
  },

  async resolveAnomaly(anomalyId: string, resolutionNote: string) {
    await api.post(`/api/anomalies/${anomalyId}/resolve`, { resolutionNote });
  },

  async dismissAnomaly(anomalyId: string, reason: string) {
    await api.post(`/api/anomalies/${anomalyId}/dismiss`, { reason });
  },

  async importExcel(file: File) {
    const form = new FormData();
    form.append('file', file);
    const res = await api.post('/api/import/excel', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data; // ImportResult
  },

  async recentImportRuns(limit = 20): Promise<ImportRun[]> {
    const res = await api.get<ImportRun[]>(`/api/import/runs/recent?limit=${limit}`);
    return res.data;
  },
};
