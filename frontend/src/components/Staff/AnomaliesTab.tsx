import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { staffService } from '../../services/staffService';

interface AnomalyEventDTO {
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
}

export default function AnomaliesTab() {
  const [anomalies, setAnomalies] = useState<AnomalyEventDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Filters
  const [statusFilter, setStatusFilter] = useState('OPEN');
  const [severityFilter, setSeverityFilter] = useState('');

  // Resolution dialog
  const [resolveDialog, setResolveDialog] = useState(false);
  const [selectedAnomaly, setSelectedAnomaly] = useState<AnomalyEventDTO | null>(null);
  const [resolutionNote, setResolutionNote] = useState('');
  const [dismissReason, setDismissReason] = useState('');

  useEffect(() => {
    loadAnomalies();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, severityFilter]);

  const loadAnomalies = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await staffService.listAnomalies(statusFilter || undefined, severityFilter || undefined);
      setAnomalies(data as AnomalyEventDTO[]);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load anomalies');
    } finally {
      setLoading(false);
    }
  };

  const handleRunDetection = async (meterId?: string) => {
    if (!confirm('Run anomaly detection now? This may take a few moments.')) {
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await staffService.runAnomalyDetection(meterId);
      setSuccess('Anomaly detection complete');
      await loadAnomalies();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Detection failed');
    } finally {
      setLoading(false);
    }
  };

  const handleResolve = async () => {
    if (!selectedAnomaly) return;

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await staffService.resolveAnomaly(selectedAnomaly.id, resolutionNote);
      setSuccess(`Anomaly ${selectedAnomaly.id.slice(0, 8)} resolved`);
      setResolveDialog(false);
      setResolutionNote('');
      await loadAnomalies();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to resolve anomaly');
    } finally {
      setLoading(false);
    }
  };

  const handleDismiss = async () => {
    if (!selectedAnomaly) return;

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await staffService.dismissAnomaly(selectedAnomaly.id, dismissReason);
      setSuccess(`Anomaly ${selectedAnomaly.id.slice(0, 8)} dismissed`);
      setResolveDialog(false);
      setDismissReason('');
      await loadAnomalies();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to dismiss anomaly');
    } finally {
      setLoading(false);
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'HIGH':
        return 'error';
      case 'MEDIUM':
        return 'warning';
      case 'LOW':
        return 'info';
      default:
        return 'default';
    }
  };

  const getTypeLabel = (type: string) => {
    return type.replace(/_/g, ' ');
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h5">Anomaly Detection</Typography>
        <Button variant="contained" onClick={() => handleRunDetection()} disabled={loading}>
          Run Detection Now
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

      {/* Filters */}
      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel>Status</InputLabel>
          <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} label="Status">
            <MenuItem value="">All</MenuItem>
            <MenuItem value="OPEN">Open</MenuItem>
            <MenuItem value="INVESTIGATING">Investigating</MenuItem>
            <MenuItem value="RESOLVED">Resolved</MenuItem>
            <MenuItem value="DISMISSED">Dismissed</MenuItem>
          </Select>
        </FormControl>

        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel>Severity</InputLabel>
          <Select value={severityFilter} onChange={(e) => setSeverityFilter(e.target.value)} label="Severity">
            <MenuItem value="">All</MenuItem>
            <MenuItem value="HIGH">High</MenuItem>
            <MenuItem value="MEDIUM">Medium</MenuItem>
            <MenuItem value="LOW">Low</MenuItem>
          </Select>
        </FormControl>

        <Button variant="outlined" onClick={loadAnomalies}>
          Refresh
        </Button>
      </Stack>

      {/* Anomalies Table */}
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>Customer ID</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Severity</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {anomalies.map((anomaly) => (
              <TableRow key={anomaly.id} hover>
                <TableCell>{anomaly.eventDate}</TableCell>
                <TableCell>
                  <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                    {anomaly.customerId.slice(0, 8)}...
                  </Typography>
                </TableCell>
                <TableCell>
                  <Chip label={getTypeLabel(anomaly.eventType)} size="small" variant="outlined" />
                </TableCell>
                <TableCell>
                  <Chip
                    label={anomaly.severity}
                    color={getSeverityColor(anomaly.severity) as any}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  <Typography variant="body2" sx={{ maxWidth: 300 }}>
                    {anomaly.description}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Chip label={anomaly.status} size="small" />
                </TableCell>
                <TableCell align="right">
                  {(anomaly.status === 'OPEN' || anomaly.status === 'INVESTIGATING') && (
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        onClick={() => {
                          setSelectedAnomaly(anomaly);
                          setResolveDialog(true);
                        }}
                      >
                        Resolve
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => {
                          setSelectedAnomaly(anomaly);
                          setResolveDialog(true);
                        }}
                      >
                        Dismiss
                      </Button>
                    </Stack>
                  )}
                </TableCell>
              </TableRow>
            ))}

            {anomalies.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No anomalies found
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Resolve/Dismiss Dialog */}
      <Dialog open={resolveDialog} onClose={() => setResolveDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Manage Anomaly</DialogTitle>
        <DialogContent>
          {selectedAnomaly && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {selectedAnomaly.eventType} - {selectedAnomaly.eventDate}
              </Typography>
              <Typography variant="body1" gutterBottom>
                {selectedAnomaly.description}
              </Typography>

              <TextField
                label="Resolution Note"
                multiline
                rows={3}
                fullWidth
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                sx={{ mt: 2, mb: 2 }}
                placeholder="Describe what was done to resolve this anomaly..."
              />

              <TextField
                label="Dismiss Reason (if dismissing)"
                multiline
                rows={2}
                fullWidth
                value={dismissReason}
                onChange={(e) => setDismissReason(e.target.value)}
                placeholder="Why is this being dismissed as a false positive?"
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResolveDialog(false)}>Cancel</Button>
          <Button
            onClick={handleDismiss}
            color="warning"
            disabled={loading || !dismissReason}
          >
            Dismiss
          </Button>
          <Button
            onClick={handleResolve}
            variant="contained"
            color="success"
            disabled={loading || !resolutionNote}
          >
            Resolve
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
}