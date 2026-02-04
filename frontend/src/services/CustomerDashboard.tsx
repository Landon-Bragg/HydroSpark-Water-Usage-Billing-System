import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Grid,
  Paper,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { customerService, UsageDataPoint, BillSummary, UsageForecast, CustomerStats } from '../../services/customerService';
import { useAuth } from '../../contexts/AuthContext';
import BillHistory from './BillHistory';

function CustomerDashboard() {
  const { user, logout } = useAuth();
  const customerId = user?.customerId || '';

  const [tab, setTab] = useState(0);

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
        customerService.getUsageHistory(customerId, 30),
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
      const bills = await customerService.getCurrentBill(customerId);
      setBillSummary(bills);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to generate forecast');
    } finally {
      setLoading(false);
    }
  };

  const currentBill = billSummary?.currentBill ?? 0;
  const estNext = billSummary?.estimatedNextBill ?? 0;

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Customer Portal
          </Typography>
          {user && (
            <Typography variant="body2" color="text.secondary">
              {user.email}
            </Typography>
          )}
        </Box>
        <Button variant="outlined" onClick={logout}>
          Logout
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Paper sx={{ mb: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} indicatorColor="primary" textColor="primary">
          <Tab label="Dashboard" />
          <Tab label="Bill History" />
        </Tabs>
      </Paper>

      {tab === 0 && (
        <>
          <Grid container spacing={3} sx={{ mb: 3 }}>
            {/* Active Meters Card */}
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" color="textSecondary">
                    Active Meters
                  </Typography>
                  <Typography variant="h3" color="primary">
                    {stats ? `${stats.activeMeters}/${stats.totalMeters}` : '—'}
                  </Typography>
                  <Typography variant="body2">Meters in Service</Typography>
                </CardContent>
              </Card>
            </Grid>

            {/* Current Bill Card */}
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" color="textSecondary">
                    Current Bill
                  </Typography>
                  <Typography variant="h3" color="error">
                    ${Number(currentBill).toFixed(2)}
                  </Typography>
                  <Typography variant="body2">Due: {billSummary?.dueDate || '—'}</Typography>
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
                  <Typography variant="body2">Based on recent usage</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Usage Chart */}
          <Paper sx={{ p: 3, mb: 3 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
              <Typography variant="h6">Daily Water Usage (Last 30 Days)</Typography>
              <Button variant="outlined" size="small" onClick={loadDashboardData} disabled={loading}>
                Refresh
              </Button>
            </Stack>

            {usageData.length > 0 ? (
              <ResponsiveContainer width="100%" height={320}>
                <LineChart data={usageData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="date"
                    tickFormatter={(date) => {
                      const d = new Date(date);
                      return `${d.getMonth() + 1}/${d.getDate()}`;
                    }}
                  />
                  <YAxis label={{ value: 'CCF', angle: -90, position: 'insideLeft' }} />
                  <Tooltip
                    labelFormatter={(date) => new Date(date).toLocaleDateString()}
                    formatter={(value: any) => [`${Number(value).toFixed(2)} CCF`, 'Usage']}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="usageCcf"
                    stroke="#1976d2"
                    name="Daily Usage"
                    strokeWidth={2}
                    dot={{ r: 3 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Alert severity="info">
                No usage data available yet. Usage data will appear after meter readings are imported.
              </Alert>
            )}
          </Paper>

          {/* Forecast Card */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Usage Forecast
            </Typography>

            {forecast ? (
              <Box>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <Typography variant="body2" color="text.secondary">
                      Forecast Period
                    </Typography>
                    <Typography variant="body1">
                      {forecast.targetPeriodStart} to {forecast.targetPeriodEnd}
                    </Typography>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Typography variant="body2" color="text.secondary">
                      Predicted Usage
                    </Typography>
                    <Typography variant="body1">
                      {Number(forecast.predictedTotalCcf).toFixed(2)} CCF
                    </Typography>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Typography variant="body2" color="text.secondary">
                      Estimated Cost
                    </Typography>
                    <Typography variant="body1" color="primary" sx={{ fontWeight: 'bold' }}>
                      ${Number(forecast.predictedTotalAmount).toFixed(2)}
                    </Typography>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Typography variant="body2" color="text.secondary">
                      Confidence
                    </Typography>
                    <Typography variant="body1">{forecast.confidenceLevel}</Typography>
                  </Grid>
                </Grid>

                <Button
                  variant="outlined"
                  onClick={handleGenerateForecast}
                  disabled={loading}
                  sx={{ mt: 2 }}
                >
                  Regenerate Forecast
                </Button>
              </Box>
            ) : (
              <Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  No forecast available yet. Generate a forecast to see predicted usage and costs.
                </Typography>
                <Button variant="contained" onClick={handleGenerateForecast} disabled={loading}>
                  Generate Forecast
                </Button>
              </Box>
            )}
          </Paper>
        </>
      )}

      {tab === 1 && <BillHistory />}
    </Container>
  );
}

export default CustomerDashboard;
