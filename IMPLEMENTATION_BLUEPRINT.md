# HydroSpark Implementation Blueprint

This document provides a complete blueprint for implementing the HydroSpark billing system. Due to the size (100+ files), I'm providing you with:

1. **Core working files** (database, configuration, main classes)
2. **Detailed implementation guide** for each component
3. **Code templates** you can copy/paste
4. **Testing strategy**

## What's Already Created

✅ Database schema (V1__Initial_Schema.sql)
✅ Seed data with users and rate plans (V2__Seed_Data.sql)
✅ Docker Compose for MySQL
✅ Application properties
✅ Maven POM with all dependencies
✅ Project structure

## What You Need to Build

The system requires approximately 100 Java files and 50 React/TypeScript files. Here's the organized breakdown:

---

## BACKEND IMPLEMENTATION (Java Spring Boot)

### Phase 1: Core Domain Models (10 files)

**Location:** `backend/src/main/java/com/hydrospark/billing/model/`

Create these JPA entities matching your database schema:

1. **User.java** - User authentication and roles
2. **Customer.java** - Customer information
3. **Meter.java** - Service locations/meters
4. **MeterReading.java** - Daily usage readings
5. **RatePlan.java** - Billing rate configurations
6. **RateComponent.java** - Individual rate pieces (tiers, fees, etc.)
7. **BillingPeriod.java** - Monthly billing cycles
8. **Bill.java** - Customer bills
9. **BillLineItem.java** - Itemized charges
10. **AnomalyEvent.java** - Usage anomalies

### Template for Entity Files:

```java
package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType;
    
    private String phone;
    private String email;
    
    @Column(name = "mailing_address_line1")
    private String mailingAddressLine1;
    
    @Column(name = "mailing_city")
    private String mailingCity;
    
    @Column(name = "mailing_state", length = 2)
    private String mailingState;
    
    @Column(name = "mailing_zip")
    private String mailingZip;
    
    @Column(name = "billing_cycle_number", nullable = false)
    private Integer billingCycleNumber;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum CustomerType {
        RESIDENTIAL, COMMERCIAL
    }
}
```

**APPLY THIS PATTERN** to create all 10 entity files matching your schema.

---

### Phase 2: Repositories (10 files)

**Location:** `backend/src/main/java/com/hydrospark/billing/repository/`

Create Spring Data JPA repositories for each entity:

```java
package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
}
```

Create similar repositories for:
1. UserRepository
2. CustomerRepository  
3. MeterRepository
4. MeterReadingRepository (with custom queries for date ranges)
5. RatePlanRepository
6. RateComponentRepository
7. BillingPeriodRepository
8. BillRepository
9. AnomalyEventRepository
10. ImportRunRepository

---

### Phase 3: Security Configuration (5 files)

**Location:** `backend/src/main/java/com/hydrospark/billing/security/`

1. **SecurityConfig.java** - Main security configuration
2. **JwtTokenProvider.java** - JWT generation/validation
3. **JwtAuthenticationFilter.java** - Request filter
4. **CustomUserDetailsService.java** - Load user details
5. **SecurityUtil.java** - Utility methods

---

### Phase 4: Core Services (15 files)

**Location:** `backend/src/main/java/com/hydrospark/billing/service/`

Business logic services:

1. **AuthService.java** - Login, registration, token refresh
2. **CustomerService.java** - Customer CRUD operations
3. **MeterService.java** - Meter management
4. **ImportService.java** - **CRITICAL**: Import 2M rows from Excel
5. **RateEngineService.java** - **CRITICAL**: Calculate bills using rate plans
6. **BillingService.java** - Generate bills, issue bills
7. **ForecastService.java** - Predict next bill amount
8. **AnomalyDetectionService.java** - Detect usage spikes/leaks
9. **BillDeliveryService.java** - Send bills via email/portal
10. **UserService.java** - User management
11. **AuditService.java** - Audit logging
12. **ScheduledTaskService.java** - Cron jobs (nightly anomaly scan, etc.)
13. **RatePlanService.java** - Rate plan configuration
14. **UsageAnalyticsService.java** - Historical usage charts
15. **SupportService.java** - Support notes

**CRITICAL SERVICE - ImportService.java:**

```java
@Service
@Slf4j
public class ImportService {
    
    @Autowired
    private MeterReadingRepository readingRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private MeterRepository meterRepository;
    
    @Transactional
    public ImportResult importFromExcel(InputStream inputStream, String filename) throws IOException {
        log.info("Starting import from file: {}", filename);
        
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheet("DailyUsage");
        
        ImportResult result = new ImportResult();
        int batchSize = 1000;
        List<MeterReading> batch = new ArrayList<>();
        
        // Skip header row
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            try {
                // Parse row data
                String customerName = getCellValue(row, 0);
                String mailingAddress = getCellValue(row, 1);
                String locationId = getCellValue(row, 2);
                String customerType = getCellValue(row, 3);
                int cycleNumber = (int) row.getCell(4).getNumericCellValue();
                String phone = getCellValue(row, 5);
                int year = (int) row.getCell(8).getNumericCellValue();
                int month = (int) row.getCell(9).getNumericCellValue();
                int day = (int) row.getCell(10).getNumericCellValue();
                double usageCcf = row.getCell(11).getNumericCellValue();
                
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
                    readingRepository.saveAll(batch);
                    batch.clear();
                    result.incrementInserted(batchSize);
                }
                
                if (i % 50000 == 0) {
                    log.info("Processed {} rows...", i);
                }
                
            } catch (Exception e) {
                log.error("Error processing row {}: {}", i, e.getMessage());
                result.incrementRejected();
            }
        }
        
        // Save remaining batch
        if (!batch.isEmpty()) {
            readingRepository.saveAll(batch);
            result.incrementInserted(batch.size());
        }
        
        workbook.close();
        log.info("Import complete: {}", result);
        return result;
    }
    
    // Helper methods...
}
```

---

### Phase 5: REST Controllers (10 files)

**Location:** `backend/src/main/java/com/hydrospark/billing/controller/`

REST API endpoints:

1. **AuthController.java** - /api/auth/*
2. **CustomerController.java** - /api/customers/*
3. **MeterController.java** - /api/meters/*
4. **ImportController.java** - /api/imports/* (file upload)
5. **RatePlanController.java** - /api/rate-plans/*
6. **BillingController.java** - /api/billing/*
7. **BillController.java** - /api/bills/*
8. **ForecastController.java** - /api/forecasts/*
9. **AnomalyController.java** - /api/anomalies/*
10. **AuditController.java** - /api/audit/*

---

## FRONTEND IMPLEMENTATION (React + TypeScript)

### Phase 1: Setup & Configuration (5 files)

1. **package.json** - Dependencies
2. **tsconfig.json** - TypeScript config
3. **src/App.tsx** - Main app component
4. **src/index.tsx** - Entry point
5. **src/services/api.ts** - Axios HTTP client

---

### Phase 2: Authentication & Routing (5 files)

1. **src/contexts/AuthContext.tsx** - Auth state management
2. **src/hooks/useAuth.ts** - Auth hook
3. **src/components/common/PrivateRoute.tsx** - Protected routes
4. **src/components/common/Login.tsx** - Login page
5. **src/App.tsx** - Route configuration

---

### Phase 3: Customer Portal (10 files)

**Dashboard:**
- src/pages/customer/Dashboard.tsx

**Usage:**
- src/pages/customer/UsageHistory.tsx
- src/components/customer/DailyUsageChart.tsx
- src/components/customer/MonthlyUsageChart.tsx

**Bills:**
- src/pages/customer/Bills.tsx
- src/components/customer/BillDetail.tsx

**Forecast:**
- src/components/customer/ForecastCard.tsx

**Profile:**
- src/pages/customer/Profile.tsx

---

### Phase 4: Staff Application (15 files)

**Admin Dashboard:**
- src/pages/staff/Dashboard.tsx

**Imports:**
- src/pages/staff/Imports.tsx
- src/components/staff/FileUploader.tsx

**Customers:**
- src/pages/staff/Customers.tsx
- src/components/staff/CustomerDetail.tsx

**Rate Plans:**
- src/pages/staff/RatePlans.tsx
- src/components/staff/RatePlanEditor.tsx
- src/components/staff/TierEditor.tsx

**Billing:**
- src/pages/staff/Billing.tsx
- src/components/staff/BillingPeriodGenerator.tsx
- src/components/staff/BillingRunner.tsx

**Anomalies:**
- src/pages/staff/Anomalies.tsx
- src/components/staff/AnomalyDetail.tsx

**Users:**
- src/pages/staff/Users.tsx
- src/components/staff/UserForm.tsx

---

## IMPLEMENTATION TIMELINE

**For someone new to Java/databases, expect:**

### Week 1: Setup & Core Infrastructure
- Day 1-2: Install prerequisites, setup database
- Day 3-4: Create all entity models
- Day 5-7: Create all repositories and test database connectivity

### Week 2: Backend Business Logic
- Day 1-2: Implement SecurityConfig and JWT auth
- Day 3-4: Build ImportService (CRITICAL for your 2M rows)
- Day 5-7: Build RateEngineService and BillingService

### Week 3: REST APIs
- Day 1-3: Build all controllers
- Day 4-5: Test APIs with Postman/Swagger
- Day 6-7: Implement scheduled jobs (anomaly detection)

### Week 4: Frontend Development
- Day 1-2: Setup React app, authentication
- Day 3-4: Build customer portal pages
- Day 5-7: Build staff application pages

### Week 5: Integration & Testing
- Day 1-2: End-to-end testing
- Day 3-4: Import your 2M rows and run full billing cycle
- Day 5-7: Bug fixes, polish UI

---

## QUICK WINS TO START

### Step 1: Get Database Running (30 minutes)
```bash
cd hydrospark-system
docker-compose up -d
```

### Step 2: Verify Database Schema (10 minutes)
```bash
docker exec -it hydrospark-mysql mysql -u hydrospark -phydrospark123 hydrospark

SHOW TABLES;  -- Should show 17 tables
DESCRIBE customers;
DESCRIBE meter_readings;
SELECT * FROM users;  -- Should show 5 default users
```

### Step 3: Create One Working Endpoint (2 hours)

Build just the health check endpoint to prove the system works:

**HealthController.java:**
```java
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    @GetMapping
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "HydroSpark Billing",
            "timestamp", LocalDateTime.now().toString()
        );
    }
}
```

Run it:
```bash
cd backend
./mvnw spring-boot:run
```

Test:
```bash
curl http://localhost:8080/api/health
```

### Step 4: Create One Entity + Repository + Test (3 hours)

Focus on Customer entity:
1. Create Customer.java (entity)
2. Create CustomerRepository.java  
3. Write a simple test:

```java
@SpringBootTest
class CustomerRepositoryTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void testFindCustomer() {
        Optional<Customer> customer = customerRepository.findById("00000000-0000-0000-0000-000000000100");
        assertTrue(customer.isPresent());
        assertEquals("Demo Customer", customer.get().getName());
    }
}
```

---

## CODE GENERATION HELPER

I can generate specific files for you! Just ask:

- "Generate the MeterReading entity"
- "Generate the ImportService"
- "Generate the AuthController"
- "Generate the customer Dashboard page"

Each request will give you complete, working code you can copy/paste.

---

## TESTING YOUR SYSTEM

### Test Import (with small file first)

1. Create a test Excel file with 100 rows
2. Use ImportController to upload
3. Verify data in database
4. Scale up to full 2M rows

### Test Billing

1. Generate billing period for January 2024
2. Run billing for cycle 20
3. Verify bills created in database
4. Check bill amounts match rate plan

### Test Customer Portal

1. Login as customer@hydrospark.com
2. View usage chart
3. See estimated next bill
4. View bill details

---

## GETTING HELP

**If you get stuck:**

1. Start with the database - verify it's working
2. Build one entity at a time
3. Test each component before moving on
4. Use Swagger UI to test APIs
5. Check logs for errors

**I'm here to help! Ask for:**
- Specific code files
- Debugging help
- Architecture clarification
- Implementation tips

---

## REMEMBER

This is a **professional, production-grade system**. Take it step by step:

✅ Database first (DONE)
✅ One entity + repository (start here)
✅ Authentication (critical for security)
✅ Import service (critical for your 2M rows)
✅ Rate engine (critical for billing)
✅ REST APIs
✅ Frontend

You've got this! 🚀

