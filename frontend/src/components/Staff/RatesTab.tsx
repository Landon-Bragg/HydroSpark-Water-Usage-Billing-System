import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Divider,
  Grid,
  List,
  ListItem,
  ListItemText,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { staffService } from '../../services/staffService';

export function RatesTab() {
  const [ratePlans, setRatePlans] = useState<any[]>([]);
  const [calcPlanId, setCalcPlanId] = useState('');
  const [calcUsage, setCalcUsage] = useState('10.0');
  const [calcResult, setCalcResult] = useState<any | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadRates();
  }, []);

  const loadRates = async () => {
    setLoading(true);
    setError(null);
    try {
      setRatePlans(await staffService.listRatePlans());
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load rate plans');
    } finally {
      setLoading(false);
    }
  };

  const doCalculate = async () => {
    setError(null);
    setLoading(true);
    try {
      const res = await staffService.calculateCharges(calcPlanId, calcUsage);
      setCalcResult(res);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Rate calculation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Grid container spacing={2}>
      <Grid item xs={12} md={6}>
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" gutterBottom>
            Rate Plans
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          <List dense>
            {ratePlans.map((p: any) => (
              <ListItem key={p.id} divider>
                <ListItemText
                  primary={`${p.name ?? '(unnamed)'} • ${p.status ?? ''}`}
                  secondary={`ID: ${p.id}  |  Scope: ${p.customerTypeScope ?? '—'}  |  Start: ${
                    p.effectiveStartDate ?? '—'
                  }`}
                />
              </ListItem>
            ))}
            {ratePlans.length === 0 && !loading && (
              <ListItem>
                <ListItemText primary="No rate plans loaded yet." />
              </ListItem>
            )}
          </List>
        </Paper>
      </Grid>

      <Grid item xs={12} md={6}>
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" gutterBottom>
            Quick Charge Calculator
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Uses POST /api/rates/calculate with usageCcf.
          </Typography>

          <Stack spacing={2}>
            <TextField
              label="Rate Plan ID"
              value={calcPlanId}
              onChange={(e) => setCalcPlanId(e.target.value)}
              placeholder="paste plan UUID"
              fullWidth
            />
            <TextField
              label="Usage (CCF)"
              value={calcUsage}
              onChange={(e) => setCalcUsage(e.target.value)}
              fullWidth
            />
            <Button variant="contained" onClick={doCalculate} disabled={!calcPlanId || loading}>
              Calculate
            </Button>
          </Stack>

          {calcResult && (
            <Box sx={{ mt: 2 }}>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="subtitle1">Total: ${calcResult.totalAmount}</Typography>
              <List dense>
                {(calcResult.lineItems ?? []).map((li: any, idx: number) => (
                  <ListItem key={idx}>
                    <ListItemText primary={li.description} secondary={`${li.category}: $${li.amount}`} />
                  </ListItem>
                ))}
              </List>
            </Box>
          )}
        </Paper>
      </Grid>
    </Grid>
  );
}

export default RatesTab;