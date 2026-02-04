import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  List,
  ListItem,
  ListItemText,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { staffService } from '../../services/staffService';

export default function CustomersTab() {
  const [search, setSearch] = useState('');
  const [customers, setCustomers] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadCustomers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadCustomers = async () => {
    setLoading(true);
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
    } finally {
      setLoading(false);
    }
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Customers
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          label="Search (name/email)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          fullWidth
          onKeyPress={(e) => {
            if (e.key === 'Enter') loadCustomers();
          }}
        />
        <Button variant="contained" onClick={loadCustomers} disabled={loading}>
          Search
        </Button>
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
        {customers.length === 0 && !loading && (
          <ListItem>
            <ListItemText primary="No customers found." />
          </ListItem>
        )}
      </List>
    </Paper>
  );
}
