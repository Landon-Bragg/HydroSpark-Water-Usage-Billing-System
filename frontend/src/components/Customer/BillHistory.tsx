// frontend/src/components/Customer/BillHistory.tsx
function BillHistory() {
  const [bills, setBills] = useState([]);
  
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Bill #</TableCell>
            <TableCell>Period</TableCell>
            <TableCell>Usage (CCF)</TableCell>
            <TableCell>Amount</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {bills.map(bill => (
            <TableRow key={bill.id}>
              <TableCell>{bill.billNumber}</TableCell>
              <TableCell>{bill.billingPeriodStart} - {bill.billingPeriodEnd}</TableCell>
              <TableCell>{bill.usageGallons}</TableCell>
              <TableCell>${bill.totalAmount}</TableCell>
              <TableCell>
                <Chip label={bill.status} />
              </TableCell>
              <TableCell>
                <Button>Download PDF</Button>
                {bill.status !== 'PAID' && <Button>Pay Now</Button>}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}