import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import { billingPeriodService, BillingPeriod, BillingRunResult } from '../../services/billingPeriodService';

export default function BillingTab() {
  const [periods, setPeriods] = useState<BillingPeriod[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Generate period dialog
  const [generateDialog, setGenerateDialog] = useState(false);
  const [cycleNumber, setCycleNumber] = useState('20');

  // Billing run result
  const [billingResult, setBillingResult] = useState<BillingRunResult | null>(null);

  useEffect(() => {
    loadPeriods();
  }, []);

  const loadPeriods = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await billingPeriodService.getAllPeriods();
      // Sort by period start date descending
      data.sort((a, b) => b.periodStartDate.localeCompare(a.periodStartDate));
      setPeriods(data);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load billing periods');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateMonthly = async () => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await billingPeriodService.generateMonthlyPeriod(Number(cycleNumber));
      setSuccess(`Monthly billing period generated for cycle ${cycleNumber}`);
      setGenerateDialog(false);
      await loadPeriods();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to generate period');
    } finally {
      setLoading(false);
    }
  };

  const handleRunBilling = async (periodId: string) => {
    if (!confirm('Run billing for this period? This will generate bills for all customers.')) {
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);
    setBillingResult(null);

    try {
      const result = await billingPeriodService.runBilling(periodId);
      setBillingResult(result);
      setSuccess(
        `Billing complete: ${result.successCount} successful, ${result.failedCount} failed, Total: $${result.totalAmount}`
      );
      await loadPeriods();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to run billing');
    } finally {
      setLoading(false);
    }
  };

  const handleIssueBills = async (periodId: string) => {
    if (!confirm('Issue all bills for this period? Bills will be marked as ISSUED.')) {
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await billingPeriodService.issueBills(periodId);
      setSuccess('Bills issued successfully');
      await loadPeriods();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to issue bills');
    } finally {
      setLoading(false);
    }
  };

  const handleSendBills = async (periodId: string) => {
    if (!confirm('Send bills via email for this period?')) {
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await billingPeriodService.sendBills(periodId);
      setSuccess(`Sent ${result.sentCount} of ${result.totalBills} bills`);
      await loadPeriods();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to send bills');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'OPEN':
        return 'info';
      case 'BILLED':
        return 'success';
      case 'CLOSED':
        return 'default';
      default:
        return 'default';
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h5">Billing Periods</Typography>
        <Button variant="contained" onClick={() => setGenerateDialog(true)} disabled={loading}>
          Generate New Period
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {billingResult && billingResult.errors.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          <Typography variant="subtitle2">Billing Errors:</Typography>
          <List dense>
            {billingResult.errors.slice(0, 5).map((err, idx) => (
              <ListItem key={idx}>
                <ListItemText
                  primary={`Customer ${err.customerId}`}
                  secondary={err.message}
                />
              </ListItem>
            ))}
            {billingResult.errors.length > 5 && (
              <ListItem>
                <ListItemText secondary={`... and ${billingResult.errors.length - 5} more errors`} />
              </ListItem>
            )}
          </List>
        </Alert>
      )}

      <Grid container spacing={2}>
        {periods.map((period) => (
          <Grid item xs={12} key={period.id}>
            <Card variant="outlined">
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Box>
                    <Typography variant="h6">
                      Cycle {period.cycleNumber} - {period.periodStartDate} to {period.periodEndDate}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Period ID: {period.id}
                    </Typography>
                    <Chip
                      label={period.status}
                      color={getStatusColor(period.status) as any}
                      size="small"
                      sx={{ mt: 1 }}
                    />
                  </Box>

                  <Stack direction="row" spacing={1}>
                    {period.status === 'OPEN' && (
                      <Button
                        variant="contained"
                        color="primary"
                        onClick={() => handleRunBilling(period.id)}
                        disabled={loading}
                      >
                        Run Billing
                      </Button>
                    )}

                    {period.status === 'BILLED' && (
                      <>
                        <Button
                          variant="contained"
                          color="secondary"
                          onClick={() => handleIssueBills(period.id)}
                          disabled={loading}
                        >
                          Issue Bills
                        </Button>
                        <Button
                          variant="outlined"
                          onClick={() => handleSendBills(period.id)}
                          disabled={loading}
                        >
                          Send Emails
                        </Button>
                      </>
                    )}
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}

        {periods.length === 0 && !loading && (
          <Grid item xs={12}>
            <Alert severity="info">
              No billing periods found. Click "Generate New Period" to create one.
            </Alert>
          </Grid>
        )}
      </Grid>

      {/* Generate Period Dialog */}
      <Dialog open={generateDialog} onClose={() => setGenerateDialog(false)}>
        <DialogTitle>Generate Monthly Billing Period</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            This will create a new billing period for the current month.
          </Typography>
          <TextField
            label="Cycle Number"
            type="number"
            value={cycleNumber}
            onChange={(e) => setCycleNumber(e.target.value)}
            fullWidth
            helperText="Billing cycle number (e.g., 20 for the 20th of each month)"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGenerateDialog(false)}>Cancel</Button>
          <Button onClick={handleGenerateMonthly} variant="contained" disabled={loading}>
            Generate
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
}
