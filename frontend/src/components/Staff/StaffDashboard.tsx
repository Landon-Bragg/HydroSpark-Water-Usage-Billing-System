import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Container,
  Paper,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';

// Import tab components
import CustomersTab from './CustomersTab';
import RatesTab from './RatesTab';
import ImportTab from './ImportTab';
import BillingTab from './BillingTab';
import AnomaliesTab from './AnomaliesTab';

type TabKey = 0 | 1 | 2 | 3 | 4;

export default function StaffDashboard() {
  const { user, logout } = useAuth();
  const [tab, setTab] = useState<TabKey>(0);

  const handleTabChange = (_: any, value: TabKey) => {
    setTab(value);
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
          <Button variant="outlined" onClick={logout}>
            Logout
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ mb: 2 }}>
        <Tabs value={tab} onChange={handleTabChange} indicatorColor="primary" textColor="primary">
          <Tab label="Customers" />
          <Tab label="Billing" />
          <Tab label="Rates" />
          <Tab label="Anomalies" />
          <Tab label="Import" />
        </Tabs>
      </Paper>

      <Box>
        {tab === 0 && <CustomersTab />}
        {tab === 1 && <BillingTab />}
        {tab === 2 && <RatesTab />}
        {tab === 3 && <AnomaliesTab />}
        {tab === 4 && <ImportTab />}
      </Box>
    </Container>
  );
}
