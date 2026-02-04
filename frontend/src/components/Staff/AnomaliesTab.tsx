// frontend/src/components/Staff/AnomaliesTab.tsx
function AnomaliesTab() {
  const [anomalies, setAnomalies] = useState([]);
  
  useEffect(() => {
    staffService.listAnomalies('OPEN').then(setAnomalies);
  }, []);
  
  return (
    <Paper>
      <Typography variant="h6">Active Anomalies</Typography>
      
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>Customer</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Severity</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {anomalies.map(a => (
              <TableRow key={a.id}>
                <TableCell>{a.eventDate}</TableCell>
                <TableCell>{a.customerId}</TableCell>
                <TableCell>
                  <Chip label={a.eventType} size="small" />
                </TableCell>
                <TableCell>
                  <Chip 
                    label={a.severity} 
                    color={a.severity === 'HIGH' ? 'error' : 'warning'}
                    size="small" 
                  />
                </TableCell>
                <TableCell>{a.description}</TableCell>
                <TableCell>
                  <Button onClick={() => resolveAnomaly(a.id)}>
                    Resolve
                  </Button>
                  <Button onClick={() => dismissAnomaly(a.id)}>
                    Dismiss
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
}