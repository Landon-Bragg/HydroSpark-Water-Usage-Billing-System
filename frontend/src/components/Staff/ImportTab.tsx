import React, { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Grid,
  List,
  ListItem,
  ListItemText,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { staffService } from '../../services/staffService';

export function ImportTab() {
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<any | null>(null);
  const [importRuns, setImportRuns] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadImports();
  }, []);

  const loadImports = async () => {
    setLoading(true);
    setError(null);
    try {
      setImportRuns(await staffService.recentImportRuns(20));
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load import runs');
    } finally {
      setLoading(false);
    }
  };

  const doImport = async () => {
    if (!importFile) return;
    setError(null);
    setLoading(true);
    try {
      const res = await staffService.importExcel(importFile);
      setImportResult(res);
      await loadImports();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Import failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Grid container spacing={2}>
      <Grid item xs={12} md={6}>
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" gutterBottom>
            Import Excel
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Upload an XLSX containing a sheet named "DailyUsage".
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          <Stack spacing={2}>
            <input type="file" accept=".xlsx" onChange={(e) => setImportFile(e.target.files?.[0] ?? null)} />
            <Button variant="contained" onClick={doImport} disabled={!importFile || loading}>
              Run Import
            </Button>
          </Stack>

          {importResult && (
            <Alert severity="success" sx={{ mt: 2 }}>
              Import complete. Inserted: {importResult.inserted ?? '—'}, Updated: {importResult.updated ?? '—'},
              Rejected: {importResult.rejected ?? '—'}
            </Alert>
          )}
        </Paper>
      </Grid>

      <Grid item xs={12} md={6}>
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" gutterBottom>
            Recent Import Runs
          </Typography>
          <List dense>
            {importRuns.map((r: any) => (
              <ListItem key={r.id} divider>
                <ListItemText
                  primary={`${r.filename ?? '—'} • ${r.status ?? ''}`}
                  secondary={`Started: ${r.startedAt ?? '—'}  |  Rows: ${r.rowsInserted ?? 0} inserted, ${
                    r.rowsUpdated ?? 0
                  } updated, ${r.rowsRejected ?? 0} rejected`}
                />
              </ListItem>
            ))}
            {importRuns.length === 0 && !loading && (
              <ListItem>
                <ListItemText primary="No import runs yet." />
              </ListItem>
            )}
          </List>
        </Paper>
      </Grid>
    </Grid>
  );
}

export default ImportTab;