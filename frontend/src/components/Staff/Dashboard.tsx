import React, { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Container,
  Divider,
  Grid,
  Paper,
  Tab,
  Tabs,
  TextField,
  Typography,
  Alert,
  List,
  ListItem,
  ListItemText,
  Stack,
} from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';
import { staffService } from '../../services/staffService';

type TabKey = 0 | 1 | 2;

export default function StaffDashboard() {
  const { user, logout } = useAuth();
  const [tab, setTab] = useState<TabKey>(0);

  const [error, setError] = useState<string | null>(null);

  // Customers
  const [search, setSearch] = useState('');
  const [customers, setCustomers] = useState<any[]>([]);

  // Rates
  const [ratePlans, setRatePlans] = useState<any[]>([]);
  const [calcPlanId, setCalcPlanId] = useState('');
  const [calcUsage, setCalcUsage] = useState('10.0');
  const [calcResult, setCalcResult] = useState<any | null>(null);

  // Import
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<any | null>(null);
  const [importRuns, setImportRuns] = useState<any[]>([]);

  const loadCustomers = async () => {
    setError(null);
    try {
      if (search.trim()) {
        setCustomers(await staffService.searchCustomers(search.trim()));
      } else {
        const page = await staffService.listCustomers(0, 25);
        setCustomers(page.content ?? []);
      }
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load customers');
    }
  };

  const loadRates = async () => {
    setError(null);
    try {
      setRatePlans(await staffService.listRatePlans());
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load rate plans');
    }
  };

  const loadImports = async () => {
    setError(null);
    try {
      setImportRuns(await staffService.recentImportRuns(20));
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load import runs');
    }
  };

  useEffect(() => {
    // initial load for first tab
    loadCustomers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleTabChange = async (_: any, value: TabKey) => {
    setTab(value);
    if (value === 0) await loadCustomers();
    if (value === 1) await loadRates();
    if (value === 2) await loadImports();
  };

  const doCalculate = async () => {
    setError(null);
    try {
      const res = await staffService.calculateCharges(calcPlanId, calcUsage);
      setCalcResult(res);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Rate calculation failed');
    }
  };

  const doImport = async () => {
    if (!importFile) return;
    setError(null);
    try {
      const res = await staffService.importExcel(importFile);
      setImportResult(res);
      await loadImports();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Import failed');
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Box>
            <Typography variant="h5">Staff Dashboard</Typography>
            <Typography variant="body2" color="text.secondary">
              Signed in as {user?.email} ({user?.role})
            </Typography>
          </Box>
          <Button variant="outlined" onClick={logout}>Logout</Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Paper sx={{ mb: 2 }}>
        <Tabs value={tab} onChange={handleTabChange} indicatorColor="primary" textColor="primary">
          <Tab label="Customers" />
          <Tab label="Rates" />
          <Tab label="Import" />
        </Tabs>
      </Paper>

      {tab === 0 && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" gutterBottom>Customers</Typography>
          <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
            <TextField
              label="Search (name/email)"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              fullWidth
            />
            <Button variant="contained" onClick={loadCustomers}>Search</Button>
          </Stack>

          <List dense>
            {customers.map((c: any) => (
              <ListItem key={c.id} divider>
                <ListItemText
                  primary={`${c.name ?? '(no name)'} • ${c.customerType ?? ''}`}
                  secondary={`ID: ${c.id}  |  Email: ${c.email ?? '—'}  |  Cycle: ${c.billingCycleNumber ?? '—'}`}
                />
              </ListItem>
            ))}
            {customers.length === 0 && (
              <ListItem>
                <ListItemText primary="No customers found." />
              </ListItem>
            )}
          </List>
        </Paper>
      )}

      {tab === 1 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Rate Plans</Typography>
              <List dense>
                {ratePlans.map((p: any) => (
                  <ListItem key={p.id} divider>
                    <ListItemText
                      primary={`${p.name ?? '(unnamed)'} • ${p.status ?? ''}`}
                      secondary={`ID: ${p.id}  |  Scope: ${p.customerTypeScope ?? '—'}  |  Start: ${p.effectiveStartDate ?? '—'}`}
                    />
                  </ListItem>
                ))}
                {ratePlans.length === 0 && (
                  <ListItem>
                    <ListItemText primary="No rate plans loaded yet." />
                  </ListItem>
                )}
              </List>
            </Paper>
          </Grid>

          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Quick Charge Calculator</Typography>
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
                <Button variant="contained" onClick={doCalculate} disabled={!calcPlanId}>
                  Calculate
                </Button>
              </Stack>

              {calcResult && (
                <Box sx={{ mt: 2 }}>
                  <Divider sx={{ mb: 2 }} />
                  <Typography variant="subtitle1">Total: {calcResult.totalAmount}</Typography>
                  <List dense>
                    {(calcResult.lineItems ?? []).map((li: any, idx: number) => (
                      <ListItem key={idx}>
                        <ListItemText primary={li.description} secondary={`${li.category}: ${li.amount}`} />
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}
            </Paper>
          </Grid>
        </Grid>
      )}

      {tab === 2 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Import Excel</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Upload an XLSX containing a sheet named "DailyUsage".
              </Typography>

              <Stack spacing={2}>
                <input
                  type="file"
                  accept=".xlsx"
                  onChange={(e) => setImportFile(e.target.files?.[0] ?? null)}
                />
                <Button variant="contained" onClick={doImport} disabled={!importFile}>
                  Run Import
                </Button>
              </Stack>

              {importResult && (
                <Alert severity="success" sx={{ mt: 2 }}>
                  Import complete. Rows processed: {importResult.totalRows ?? '—'} (success: {importResult.successRows ?? '—'})
                </Alert>
              )}
            </Paper>
          </Grid>

          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Recent Import Runs</Typography>
              <List dense>
                {importRuns.map((r: any) => (
                  <ListItem key={r.id} divider>
                    <ListItemText
                      primary={`${r.filename ?? '—'} • ${r.status ?? ''}`}
                      secondary={`Started: ${r.startedAt ?? '—'}  |  Completed: ${r.completedAt ?? '—'}  |  Success: ${r.successRows ?? '—'} / ${r.totalRows ?? '—'}`}
                    />
                  </ListItem>
                ))}
                {importRuns.length === 0 && (
                  <ListItem><ListItemText primary="No import runs yet." /></ListItem>
                )}
              </List>
            </Paper>
          </Grid>
        </Grid>
      )}
    </Container>
  );
}
