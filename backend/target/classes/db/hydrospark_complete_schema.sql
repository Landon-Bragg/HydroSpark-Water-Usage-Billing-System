-- HydroSpark Complete Database Schema
-- Includes schema for 1M+ rows of daily water usage data
-- MySQL/MariaDB Compatible

-- ==============================================
-- DROP EXISTING TABLES
-- ==============================================
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS usage_forecasts;
DROP TABLE IF EXISTS anomaly_events;
DROP TABLE IF EXISTS bill_line_items;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS billing_periods;
DROP TABLE IF EXISTS daily_water_readings;
DROP TABLE IF EXISTS meter_readings;
DROP TABLE IF EXISTS rate_components;
DROP TABLE IF EXISTS rate_plans;
DROP TABLE IF EXISTS import_runs;
DROP TABLE IF EXISTS meters;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;

-- ==============================================
-- USER AUTHENTICATION & AUTHORIZATION
-- ==============================================
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL DEFAULT 'System',
    last_name VARCHAR(100) NOT NULL DEFAULT 'User',
    role ENUM('ADMIN', 'BILLING', 'OPERATIONS', 'SUPPORT', 'CUSTOMER') NOT NULL,
    customer_id VARCHAR(36) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    last_login_attempt TIMESTAMP NULL,
    account_locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- CUSTOMERS
-- ==============================================
CREATE TABLE customers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    
    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    business_name VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    
    -- Service Address
    service_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL DEFAULT 'TX',
    zip_code VARCHAR(10) NOT NULL,
    
    -- Mailing Address (from your CSV data)
    mailing_address_line1 VARCHAR(255),
    mailing_city VARCHAR(100),
    mailing_state VARCHAR(2),
    mailing_zip VARCHAR(10),
    
    -- Customer Classification
    customer_type ENUM('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL') DEFAULT 'RESIDENTIAL',
    billing_cycle_number INT DEFAULT 1,
    
    -- Account Status
    is_active BOOLEAN DEFAULT TRUE,
    auto_pay BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_account_number (account_number),
    INDEX idx_email (email),
    INDEX idx_user_id (user_id),
    INDEX idx_customer_type (customer_type),
    INDEX idx_billing_cycle (billing_cycle_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add foreign key to users table
ALTER TABLE users ADD CONSTRAINT fk_users_customer 
FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

-- ==============================================
-- METERS
-- ==============================================
CREATE TABLE meters (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    
    -- Meter Identification (matching your CSV Location ID)
    external_location_id VARCHAR(50) UNIQUE,
    meter_number VARCHAR(50),
    
    -- Service Location
    service_address_line1 VARCHAR(255),
    service_city VARCHAR(100),
    service_state VARCHAR(2),
    service_zip VARCHAR(10),
    facility_name VARCHAR(255) NULL,
    
    -- Meter Details
    installation_date DATE DEFAULT (CURRENT_DATE),
    meter_type VARCHAR(50) DEFAULT 'STANDARD',
    status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
    
    -- Last Reading Cache
    last_reading_date DATE,
    last_reading_value DECIMAL(12, 2),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    INDEX idx_meter_number (meter_number),
    INDEX idx_customer_id (customer_id),
    INDEX idx_external_location_id (external_location_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- DAILY WATER READINGS (FOR 1M+ ROWS)
-- ==============================================
-- This table stores the daily water usage data from your CSV
CREATE TABLE daily_water_readings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Link to customer/meter
    customer_id VARCHAR(36) NOT NULL,
    meter_id VARCHAR(36) NOT NULL,
    external_location_id VARCHAR(50) NOT NULL,
    
    -- Date Information
    reading_year INT NOT NULL,
    reading_month INT NOT NULL,
    reading_day INT NOT NULL,
    reading_date DATE NOT NULL,
    
    -- Usage (in CCF - Hundred Cubic Feet)
    daily_usage_ccf DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    
    -- Import tracking
    import_run_id VARCHAR(36) NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    
    INDEX idx_customer_date (customer_id, reading_date),
    INDEX idx_meter_date (meter_id, reading_date),
    INDEX idx_location_date (external_location_id, reading_date),
    INDEX idx_reading_date (reading_date),
    INDEX idx_year_month (reading_year, reading_month),
    UNIQUE KEY unique_meter_date (meter_id, reading_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Partition by year for performance with large datasets
-- ALTER TABLE daily_water_readings 
-- PARTITION BY RANGE (reading_year) (
--     PARTITION p2018 VALUES LESS THAN (2019),
--     PARTITION p2019 VALUES LESS THAN (2020),
--     PARTITION p2020 VALUES LESS THAN (2021),
--     PARTITION p2021 VALUES LESS THAN (2022),
--     PARTITION p2022 VALUES LESS THAN (2023),
--     PARTITION p2023 VALUES LESS THAN (2024),
--     PARTITION p2024 VALUES LESS THAN (2025),
--     PARTITION p2025 VALUES LESS THAN (2026),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- ==============================================
-- MONTHLY METER READINGS (AGGREGATED)
-- ==============================================
CREATE TABLE meter_readings (
    id VARCHAR(36) PRIMARY KEY,
    meter_id VARCHAR(36) NOT NULL,
    
    -- Reading Details
    reading_date DATE NOT NULL,
    reading_value DECIMAL(12, 2) NOT NULL,
    previous_reading_value DECIMAL(12, 2),
    usage_amount DECIMAL(12, 2),
    
    -- Reading Source
    reading_source ENUM('MANUAL', 'AUTOMATIC', 'ESTIMATED', 'IMPORT') DEFAULT 'AUTOMATIC',
    notes TEXT,
    
    -- Billing
    is_billed BOOLEAN DEFAULT FALSE,
    bill_id VARCHAR(36) NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    INDEX idx_meter_date (meter_id, reading_date),
    INDEX idx_reading_date (reading_date),
    INDEX idx_is_billed (is_billed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- RATE PLANS & PRICING
-- ==============================================
CREATE TABLE rate_plans (
    id VARCHAR(36) PRIMARY KEY,
    plan_name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Applicability
    customer_type_scope ENUM('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL', 'ALL') DEFAULT 'ALL',
    effective_date DATE NOT NULL,
    expiration_date DATE,
    
    -- Plan Status
    status ENUM('DRAFT', 'ACTIVE', 'ARCHIVED') DEFAULT 'DRAFT',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_customer_type (customer_type_scope),
    INDEX idx_effective_date (effective_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rate_components (
    id VARCHAR(36) PRIMARY KEY,
    rate_plan_id VARCHAR(36) NOT NULL,
    
    -- Component Details
    component_name VARCHAR(100) NOT NULL,
    component_type ENUM('BASE_CHARGE', 'USAGE_TIER', 'SEASONAL_SURCHARGE', 'SERVICE_FEE', 'TAX') NOT NULL,
    
    -- Pricing
    rate_amount DECIMAL(10, 4) NOT NULL,
    tier_min_usage DECIMAL(10, 2),
    tier_max_usage DECIMAL(10, 2),
    
    -- Seasonal
    season_start_month INT,
    season_end_month INT,
    
    -- Order
    display_order INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id) ON DELETE CASCADE,
    INDEX idx_rate_plan (rate_plan_id),
    INDEX idx_component_type (component_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- BILLING PERIODS & BILLS
-- ==============================================
CREATE TABLE billing_periods (
    id VARCHAR(36) PRIMARY KEY,
    
    -- Period Details
    period_name VARCHAR(100) NOT NULL,
    cycle_number INT NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    
    -- Bill Generation
    bill_generation_date DATE,
    bill_due_date DATE,
    
    -- Status
    status ENUM('OPEN', 'CLOSED', 'PROCESSING', 'COMPLETED') DEFAULT 'OPEN',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_cycle_number (cycle_number),
    INDEX idx_status (status),
    INDEX idx_period_dates (period_start_date, period_end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bills (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    billing_period_id VARCHAR(36) NOT NULL,
    
    -- Bill Details
    bill_number VARCHAR(50) UNIQUE NOT NULL,
    bill_date DATE NOT NULL,
    due_date DATE NOT NULL,
    
    -- Amounts
    total_usage DECIMAL(12, 2) DEFAULT 0,
    subtotal DECIMAL(10, 2) DEFAULT 0,
    tax_amount DECIMAL(10, 2) DEFAULT 0,
    total_amount DECIMAL(10, 2) NOT NULL,
    amount_paid DECIMAL(10, 2) DEFAULT 0,
    balance_due DECIMAL(10, 2) NOT NULL,
    
    -- Status
    status ENUM('DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED') DEFAULT 'DRAFT',
    payment_date DATE,
    payment_method VARCHAR(50),
    
    -- Delivery
    delivery_method ENUM('EMAIL', 'MAIL', 'PORTAL', 'NONE') DEFAULT 'NONE',
    email_sent_at TIMESTAMP NULL,
    
    -- Forecasting
    is_estimate BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (billing_period_id) REFERENCES billing_periods(id) ON DELETE CASCADE,
    INDEX idx_customer (customer_id),
    INDEX idx_billing_period (billing_period_id),
    INDEX idx_bill_number (bill_number),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bill_line_items (
    id VARCHAR(36) PRIMARY KEY,
    bill_id VARCHAR(36) NOT NULL,
    
    -- Line Item Details
    description VARCHAR(255) NOT NULL,
    line_type ENUM('USAGE', 'BASE_CHARGE', 'FEE', 'TAX', 'ADJUSTMENT', 'DISCOUNT') NOT NULL,
    
    -- Quantities & Rates
    quantity DECIMAL(12, 2),
    unit_price DECIMAL(10, 4),
    amount DECIMAL(10, 2) NOT NULL,
    
    -- Display
    display_order INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
    INDEX idx_bill (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- USAGE FORECASTING
-- ==============================================
CREATE TABLE usage_forecasts (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    meter_id VARCHAR(36) NOT NULL,
    
    -- Forecast Period
    forecast_start_date DATE NOT NULL,
    forecast_end_date DATE NOT NULL,
    
    -- Predicted Usage
    predicted_usage_ccf DECIMAL(12, 2) NOT NULL,
    predicted_cost DECIMAL(10, 2),
    
    -- Confidence
    confidence_level ENUM('HIGH', 'MEDIUM', 'LOW') DEFAULT 'MEDIUM',
    forecast_method ENUM('MOVING_AVERAGE', 'HISTORICAL', 'SEASONAL', 'ML_MODEL') DEFAULT 'MOVING_AVERAGE',
    
    -- Metadata
    based_on_days INT,
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    INDEX idx_customer (customer_id),
    INDEX idx_meter (meter_id),
    INDEX idx_forecast_date (forecast_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- ANOMALY DETECTION
-- ==============================================
CREATE TABLE anomaly_events (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    meter_id VARCHAR(36) NOT NULL,
    
    -- Event Details
    event_type ENUM('HIGH_USAGE', 'ZERO_USAGE', 'SUDDEN_SPIKE', 'SUSTAINED_HIGH', 'LEAK_SUSPECTED') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    
    -- Detection
    detected_date DATE NOT NULL,
    detection_method VARCHAR(100),
    
    -- Usage Details
    actual_usage DECIMAL(12, 2),
    expected_usage DECIMAL(12, 2),
    deviation_percentage DECIMAL(5, 2),
    
    -- Status & Resolution
    status ENUM('OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE') DEFAULT 'OPEN',
    resolution_notes TEXT,
    resolved_at TIMESTAMP NULL,
    resolved_by VARCHAR(36) NULL,
    
    -- Notification
    customer_notified BOOLEAN DEFAULT FALSE,
    notification_sent_at TIMESTAMP NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    INDEX idx_customer (customer_id),
    INDEX idx_meter (meter_id),
    INDEX idx_detected_date (detected_date),
    INDEX idx_status (status),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- DATA IMPORT TRACKING
-- ==============================================
CREATE TABLE import_runs (
    id VARCHAR(36) PRIMARY KEY,
    
    -- Import Details
    source_type ENUM('CSV', 'EXCEL', 'API', 'MANUAL') NOT NULL,
    source_file_name VARCHAR(255),
    
    -- Statistics
    rows_processed INT DEFAULT 0,
    rows_imported INT DEFAULT 0,
    rows_failed INT DEFAULT 0,
    
    -- Status
    status ENUM('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'IN_PROGRESS',
    error_message TEXT,
    
    -- Timing
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    -- User
    imported_by VARCHAR(36),
    
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- AUDIT LOGGING
-- ==============================================
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Action Details
    user_id VARCHAR(36),
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(36),
    
    -- Changes
    old_value TEXT,
    new_value TEXT,
    
    -- Context
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
