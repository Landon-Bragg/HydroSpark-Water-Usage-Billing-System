import React, { useEffect, useMemo, useState } from 'react';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  Button,
  Alert,
  Stack,
} from '@mui/material';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { customerService, UsageDataPoint, BillSummary, UsageForecast, CustomerStats } from '../../services/customerService';
import { useAuth } from '../../contexts/AuthContext';

function CustomerDashboard() {
  const { user, logout } = useAuth();
  const customerId = user?.customerId || '';

  const [stats, setStats] = useState<CustomerStats | null>(null);
  const [forecast, setForecast] = useState<UsageForecast | null>(null);
  const [usageData, setUsageData] = useState<UsageDataPoint[]>([]);
  const [billSummary, setBillSummary] = useState<BillSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    if (!customerId) return;
    loadDashboardData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [customerId]);

  const loadDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [s, f, usage, bills] = await Promise.all([
        customerService.getCustomerStats(customerId),
        customerService.getLatestForecast(customerId),
        customerService.getUsageHistory(customerId),
        customerService.getCurrentBill(customerId),
      ]);

      setStats(s);
      setForecast(f);
      setUsageData(usage);
      setBillSummary(bills);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Error loading dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateForecast = async () => {
    if (!customerId) return;
    setLoading(true);
    setError(null);
    try {
      const f = await customerService.generateForecast(customerId);
      setForecast(f);
      const usage = await customerService.getUsageHistory(customerId);
      const bills = await customerService.getCurrentBill(customerId);
      setUsageData(usage);
      setBillSummary(bills);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to generate forecast');
    } finally {
      setLoading(false);
    }
  };

  const estNext = billSummary?.estimatedNextBill ?? 0;

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <BoxTitle
          title="Customer Dashboard"
          subtitle={user ? `Signed in as ${user.email}` : ''}
        />
        <Button variant="outlined" onClick={logout}>Logout</Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={3}>
        {/* Stats Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="textSecondary">
                Meters
              </Typography>
              <Typography variant="h3" color="primary">
                {stats ? `${stats.activeMeters}/${stats.totalMeters}` : '—'}
              </Typography>
              <Typography variant="body2">
                Active / Total
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Estimated Next Bill Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="textSecondary">
                Estimated Next Bill
              </Typography>
              <Typography variant="h3" color="secondary">
                ${Number(estNext).toFixed(2)}
              </Typography>
              <Typography variant="body2">
                Due: {billSummary?.dueDate || '—'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Forecast Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="textSecondary">
                Forecast
              </Typography>
              <Typography variant="body2" sx={{ mb: 1 }}>
                {forecast
                  ? `Period: ${forecast.targetPeriodStart} → ${forecast.targetPeriodEnd}`
                  : 'No forecast found yet.'}
              </Typography>
              <Button variant="contained" onClick={handleGenerateForecast} disabled={loading}>
                {forecast ? 'Regenerate Forecast' : 'Generate Forecast'}
              </Button>
            </CardContent>
          </Card>
        </Grid>

        {/* Usage Chart (estimated from forecast until real readings endpoint exists) */}
        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Estimated Daily Usage (from latest forecast)
            </Typography>

            {!forecast && (
              <Alert severity="info" sx={{ mb: 2 }}>
                This chart is generated from the forecast because the backend doesn’t expose meter reading history endpoints yet.
                Once you add a readings controller, we’ll swap this to real usage.
              </Alert>
            )}

            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={usageData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis label={{ value: 'CCF', angle: -90, position: 'insideLeft' }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="usageCcf" stroke="#1976d2" name="Usage (CCF)" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>
      </Grid>
    </Container>
  );
}

function BoxTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div>
      <Typography variant="h4" gutterBottom>{title}</Typography>
      {subtitle && <Typography variant="body2" color="text.secondary">{subtitle}</Typography>}
    </div>
  );
}

export default CustomerDashboard;
