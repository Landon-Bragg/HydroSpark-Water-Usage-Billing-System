package com.hydrospark.billing.service;

import com.hydrospark.billing.model.*;
import com.hydrospark.billing.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ImportService {
    
    private final MeterReadingRepository readingRepository;
    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;
    private final ImportRunRepository importRunRepository;
    
    // Cache for performance during large imports
    private final Map<String, Customer> customerCache = new HashMap<>();
    private final Map<String, Meter> meterCache = new HashMap<>();
    
    public ImportService(MeterReadingRepository readingRepository,
                        CustomerRepository customerRepository,
                        MeterRepository meterRepository,
                        ImportRunRepository importRunRepository) {
        this.readingRepository = readingRepository;
        this.customerRepository = customerRepository;
        this.meterRepository = meterRepository;
        this.importRunRepository = importRunRepository;
    }
    
    @Transactional
    public ImportResult importFromExcel(InputStream inputStream, String filename, String userId) {
        log.info("Starting import from file: {}", filename);
        
        // Create import run record
        ImportRun importRun = ImportRun.builder()
                .filename(filename)
                .sourceType(ImportRun.SourceType.XLSX)
                .status(ImportRun.Status.IN_PROGRESS)
                .startedBy(userId)
                .build();
        importRun = importRunRepository.save(importRun);
        
        ImportResult result = new ImportResult();
        result.setImportRunId(importRun.getId());
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet("DailyUsage");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'DailyUsage' not found in Excel file");
            }
            
            int totalRows = sheet.getLastRowNum();
            log.info("Found {} rows to process", totalRows);
            
            int batchSize = 1000;
            List<MeterReading> batch = new ArrayList<>();
            
            // Skip header row (row 0)
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                try {
                    // Parse row data
                    String customerName = getCellValueAsString(row.getCell(0));
                    String mailingAddress = getCellValueAsString(row.getCell(1));
                    String locationId = getCellValueAsString(row.getCell(2));
                    String customerType = getCellValueAsString(row.getCell(3));
                    int cycleNumber = getCellValueAsInt(row.getCell(4));
                    String phone = getCellValueAsString(row.getCell(5));
                    int year = getCellValueAsInt(row.getCell(8));
                    int month = getCellValueAsInt(row.getCell(9));
                    int day = getCellValueAsInt(row.getCell(10));
                    double usageCcf = getCellValueAsDouble(row.getCell(11));
                    
                    // Validate data
                    if (usageCcf < 0) {
                        result.incrementRejected();
                        log.warn("Row {}: Negative usage rejected", i);
                        continue;
                    }
                    
                    // Find or create customer
                    Customer customer = findOrCreateCustomer(customerName, customerType, cycleNumber, phone, mailingAddress);
                    
                    // Find or create meter
                    Meter meter = findOrCreateMeter(customer, locationId, mailingAddress);
                    
                    // Create reading
                    MeterReading reading = MeterReading.builder()
                            .meterId(meter.getId())
                            .readingDate(LocalDate.of(year, month, day))
                            .usageCcf(BigDecimal.valueOf(usageCcf))
                            .source(MeterReading.ReadingSource.IMPORT_XLSX)
                            .build();
                    
                    batch.add(reading);
                    
                    // Batch insert for performance
                    if (batch.size() >= batchSize) {
                        saveBatch(batch, result);
                        batch.clear();
                    }
                    
                    // Progress logging every 50,000 rows
                    if (i % 50000 == 0) {
                        log.info("Processed {} of {} rows ({} inserted, {} updated, {} rejected)", 
                                i, totalRows, result.getInserted(), result.getUpdated(), result.getRejected());
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", i, e.getMessage());
                    result.incrementRejected();
                }
            }
            
            // Save remaining batch
            if (!batch.isEmpty()) {
                saveBatch(batch, result);
            }
            
            // Clear caches
            customerCache.clear();
            meterCache.clear();
            
            // Update import run
            importRun.setStatus(ImportRun.Status.COMPLETED);
            importRun.setTotalRows(totalRows);
            importRun.setRowsInserted(result.getInserted());
            importRun.setRowsUpdated(result.getUpdated());
            importRun.setRowsRejected(result.getRejected());
            importRun.setCompletedAt(LocalDateTime.now());
            importRunRepository.save(importRun);
            
            log.info("Import complete: {}", result);
            
        } catch (Exception e) {
            log.error("Import failed: {}", e.getMessage(), e);
            importRun.setStatus(ImportRun.Status.FAILED);
            importRun.setErrorMessage(e.getMessage());
            importRun.setCompletedAt(LocalDateTime.now());
            importRunRepository.save(importRun);
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    private void saveBatch(List<MeterReading> batch, ImportResult result) {
        for (MeterReading reading : batch) {
            try {
                // Check if reading already exists (upsert logic)
                var existing = readingRepository.findByMeterIdAndReadingDate(
                        reading.getMeterId(), reading.getReadingDate());
                
                if (existing.isPresent()) {
                    // Update existing
                    MeterReading existingReading = existing.get();
                    existingReading.setUsageCcf(reading.getUsageCcf());
                    existingReading.setSource(reading.getSource());
                    readingRepository.save(existingReading);
                    result.incrementUpdated();
                } else {
                    // Insert new
                    readingRepository.save(reading);
                    result.incrementInserted();
                }
            } catch (Exception e) {
                log.error("Error saving reading: {}", e.getMessage());
                result.incrementRejected();
            }
        }
    }
    
    private Customer findOrCreateCustomer(String name, String customerType, int cycleNumber, 
                                         String phone, String mailingAddress) {
        // Use cache for performance
        String cacheKey = name + "|" + cycleNumber;
        if (customerCache.containsKey(cacheKey)) {
            return customerCache.get(cacheKey);
        }
        
        Customer customer = customerRepository.findByEmail(name.toLowerCase().replace(" ", ".") + "@example.com")
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .name(name)
                            .customerType("RESIDENTIAL".equalsIgnoreCase(customerType) ? 
                                    Customer.CustomerType.RESIDENTIAL : Customer.CustomerType.COMMERCIAL)
                            .phone(phone)
                            .mailingAddressLine1(mailingAddress)
                            .billingCycleNumber(cycleNumber)
                            .build();
                    return customerRepository.save(newCustomer);
                });
        
        customerCache.put(cacheKey, customer);
        return customer;
    }
    
    private Meter findOrCreateMeter(Customer customer, String externalLocationId, String address) {
        // Use cache for performance
        if (meterCache.containsKey(externalLocationId)) {
            return meterCache.get(externalLocationId);
        }
        
        Meter meter = meterRepository.findByExternalLocationId(externalLocationId)
                .orElseGet(() -> {
                    Meter newMeter = Meter.builder()
                            .customerId(customer.getId())
                            .externalLocationId(externalLocationId)
                            .serviceAddressLine1(address)
                            .status(Meter.Status.ACTIVE)
                            .build();
                    return meterRepository.save(newMeter);
                });
        
        meterCache.put(externalLocationId, meter);
        return meter;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }
    
    private int getCellValueAsInt(Cell cell) {
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> Integer.parseInt(cell.getStringCellValue());
            default -> 0;
        };
    }
    
    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> Double.parseDouble(cell.getStringCellValue());
            default -> 0.0;
        };
    }
    
    // Inner result class
    @lombok.Data
    public static class ImportResult {
        private String importRunId;
        private int inserted = 0;
        private int updated = 0;
        private int rejected = 0;
        
        public void incrementInserted() { inserted++; }
        public void incrementUpdated() { updated++; }
        public void incrementRejected() { rejected++; }
        
        @Override
        public String toString() {
            return String.format("ImportResult{inserted=%d, updated=%d, rejected=%d, total=%d}", 
                    inserted, updated, rejected, inserted + updated + rejected);
        }
    }
}
