// backend/src/main/java/com/hydrospark/billing/service/BillingPeriodService.java
@Service
public class BillingPeriodService {
    
    public BillingPeriod generatePeriod(int cycleNumber, LocalDate start, LocalDate end) {
        // Create billing period
        // Validate no overlap
        // Return saved period
    }
    
    public BillingRunResult runBillingForPeriod(String periodId) {
        // Get all customers for this cycle
        // For each customer:
        //   - Get their meter(s)
        //   - Sum usage for the period
        //   - Get active rate plan
        //   - Use RateEngineService to calculate charges
        //   - Create Bill with line items
        //   - Save bill
        // Update period status
        // Return summary
    }
}