import React, { useEffect, useState } from 'react';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
} from '@mui/material';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { customerService } from '../../services/customerService';

interface UsageData {
  date: string;
  usage: number;
}

interface BillSummary {
  currentBill: number;
  estimatedNextBill: number;
  dueDate: string;
}

function CustomerDashboard() {
  const [usageData, setUsageData] = useState<UsageData[]>([]);
  const [billSummary, setBillSummary] = useState<BillSummary | null>(null);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const usage = await customerService.getUsageHistory();
      const bills = await customerService.getCurrentBill();
      
      setUsageData(usage);
      setBillSummary(bills);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>
        Water Usage Dashboard
      </Typography>

      <Grid container spacing={3}>
        {/* Current Bill Card */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="textSecondary">
                Current Bill
              </Typography>
              <Typography variant="h3" color="primary">
                ${billSummary?.currentBill.toFixed(2) || '0.00'}
              </Typography>
              <Typography variant="body2">
                Due: {billSummary?.dueDate || 'N/A'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Estimated Next Bill Card */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="textSecondary">
                Estimated Next Bill
              </Typography>
              <Typography variant="h3" color="secondary">
                ${billSummary?.estimatedNextBill.toFixed(2) || '0.00'}
              </Typography>
              <Typography variant="body2">
                Based on current usage
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Usage Chart */}
        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Daily Water Usage (Last 30 Days)
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={usageData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis label={{ value: 'Gallons', angle: -90, position: 'insideLeft' }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="usage" stroke="#1976d2" name="Usage (gallons)" />
              </LineChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>
      </Grid>
    </Container>
  );
}

export default CustomerDashboard;