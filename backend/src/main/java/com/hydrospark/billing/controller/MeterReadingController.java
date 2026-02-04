// backend/src/main/java/com/hydrospark/billing/controller/MeterReadingController.java
@RestController
@RequestMapping("/api/meter-readings")
public class MeterReadingController {
    
    @GetMapping("/meter/{meterId}")
    public List<MeterReadingDTO> getReadingsByMeter(
            @PathVariable String meterId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        // Return readings for date range
    }
    
    @GetMapping("/customer/{customerId}")
    public List<MeterReadingDTO> getReadingsByCustomer(
            @PathVariable String customerId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        // Get all customer's meters
        // Return combined readings
    }
}