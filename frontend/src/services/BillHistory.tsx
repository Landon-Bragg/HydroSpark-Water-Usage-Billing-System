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
  Divider,
  List,
  ListItem,
  ListItemText,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { customerService, BillDTO } from '../../services/customerService';
import { useAuth } from '../../contexts/AuthContext';

export default function BillHistory() {
  const { user } = useAuth();
  const customerId = user?.customerId || '';

  const [bills, setBills] = useState<BillDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Bill details dialog
  const [selectedBill, setSelectedBill] = useState<BillDTO | null>(null);
  const [detailsDialog, setDetailsDialog] = useState(false);

  useEffect(() => {
    if (customerId) {
      loadBills();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [customerId]);

  const loadBills = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await customerService.getAllBills(customerId);
      setBills(data);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load bills');
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = (bill: BillDTO) => {
    setSelectedBill(bill);
    setDetailsDialog(true);
  };

  const handlePayBill = async (billId: string, amount: number) => {
    if (!confirm(`Pay $${amount.toFixed(2)}?`)) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await customerService.payBill(billId, amount, 'CREDIT_CARD');
      alert('Payment recorded successfully!');
      await loadBills();
      setDetailsDialog(false);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Payment failed');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PAID':
        return 'success';
      case 'SENT':
      case 'ISSUED':
        return 'warning';
      case 'DRAFT':
        return 'default';
      case 'VOID':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom>
          Bill History
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Issue Date</TableCell>
                <TableCell>Period</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {bills.map((bill) => (
                <TableRow key={bill.id}>
                  <TableCell>{bill.issueDate}</TableCell>
                  <TableCell>
                    <Typography variant="body2">Bill #{bill.id.slice(0, 8)}</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="h6">${Number(bill.totalAmount).toFixed(2)}</Typography>
                  </TableCell>
                  <TableCell>{bill.dueDate}</TableCell>
                  <TableCell>
                    <Chip label={bill.status} color={getStatusColor(bill.status) as any} size="small" />
                  </TableCell>
                  <TableCell align="right">
                    <Button size="small" onClick={() => handleViewDetails(bill)}>
                      View Details
                    </Button>
                    {bill.status !== 'PAID' && bill.status !== 'VOID' && (
                      <Button
                        size="small"
                        variant="contained"
                        color="primary"
                        onClick={() => handlePayBill(bill.id, Number(bill.totalAmount))}
                        disabled={loading}
                        sx={{ ml: 1 }}
                      >
                        Pay Now
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}

              {bills.length === 0 && !loading && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    <Typography variant="body2" color="text.secondary">
                      No bills found
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Bill Details Dialog */}
      <Dialog
        open={detailsDialog}
        onClose={() => setDetailsDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Bill Details</DialogTitle>
        <DialogContent>
          {selectedBill && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Bill #{selectedBill.id.slice(0, 8)}
              </Typography>

              <Typography variant="body2" color="text.secondary" gutterBottom>
                Issue Date: {selectedBill.issueDate} | Due Date: {selectedBill.dueDate}
              </Typography>

              <Chip
                label={selectedBill.status}
                color={getStatusColor(selectedBill.status) as any}
                sx={{ mb: 2 }}
              />

              <Divider sx={{ my: 2 }} />

              <Typography variant="subtitle1" gutterBottom>
                Charges
              </Typography>

              <List dense>
                {selectedBill.lineItems.map((item) => (
                  <ListItem key={item.id}>
                    <ListItemText
                      primary={item.description}
                      secondary={item.category}
                    />
                    <Typography variant="body1">${Number(item.amount).toFixed(2)}</Typography>
                  </ListItem>
                ))}
              </List>

              <Divider sx={{ my: 2 }} />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography>Subtotal:</Typography>
                <Typography>${Number(selectedBill.subtotal).toFixed(2)}</Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography>Fees:</Typography>
                <Typography>${Number(selectedBill.totalFees).toFixed(2)}</Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography>Surcharges:</Typography>
                <Typography>${Number(selectedBill.totalSurcharges).toFixed(2)}</Typography>
              </Box>

              <Divider sx={{ my: 1 }} />

              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="h6">Total:</Typography>
                <Typography variant="h6">${Number(selectedBill.totalAmount).toFixed(2)}</Typography>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailsDialog(false)}>Close</Button>
          {selectedBill && selectedBill.status !== 'PAID' && selectedBill.status !== 'VOID' && (
            <Button
              variant="contained"
              color="primary"
              onClick={() => handlePayBill(selectedBill.id, Number(selectedBill.totalAmount))}
              disabled={loading}
            >
              Pay Now
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}
