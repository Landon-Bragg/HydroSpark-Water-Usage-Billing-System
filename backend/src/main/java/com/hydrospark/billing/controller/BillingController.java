// backend/src/main/java/com/hydrospark/billing/controller/BillingController.java

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hydrospark.billing.dto.BillDTO;
import com.hydrospark.billing.model.BillingPeriod;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    
    // Generate billing periods for a cycle
    @PostMapping("/periods/generate")
    public BillingPeriod generatePeriod(@RequestBody GeneratePeriodRequest request) {}
    
    // Run billing for a period (generate all customer bills)
    @PostMapping("/periods/{periodId}/run")
    public BillingRunResult runBilling(@PathVariable String periodId) {}
    
    // Issue bills (mark as ISSUED)
    @PostMapping("/periods/{periodId}/issue")
    public void issueBills(@PathVariable String periodId) {}
    
    // Send bills via email
    @PostMapping("/periods/{periodId}/send")
    public void sendBills(@PathVariable String periodId) {}
    
    // Get bills for a period
    @GetMapping("/periods/{periodId}/bills")
    public List<BillDTO> getBillsForPeriod(@PathVariable String periodId) {}

    // Add to existing controllers or create BillController.java
    @GetMapping("/api/customers/{customerId}/bills")
    public List<BillDTO> getCustomerBills(@PathVariable String customerId) {}

    @GetMapping("/api/bills/{billId}")
    public BillDTO getBillDetails(@PathVariable String billId) {}

    @PostMapping("/api/bills/{billId}/pay")
    public void recordPayment(@PathVariable String billId, @RequestBody PaymentRequest payment) {}
}