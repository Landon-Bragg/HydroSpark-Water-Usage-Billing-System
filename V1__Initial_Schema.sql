-- V1__Initial_Schema.sql
-- HydroSpark Water Billing System Database Schema

-- Users table
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'BILLING', 'OPERATIONS', 'SUPPORT', 'CUSTOMER') NOT NULL,
    customer_id CHAR(36) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Customers table
CREATE TABLE customers (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    customer_type ENUM('RESIDENTIAL', 'COMMERCIAL') NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    mailing_address_line1 VARCHAR(255),
    mailing_city VARCHAR(100),
    mailing_state VARCHAR(2),
    mailing_zip VARCHAR(10),
    billing_cycle_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customer_type (customer_type),
    INDEX idx_billing_cycle (billing_cycle_number),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Meters/Service Locations table
CREATE TABLE meters (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    external_location_id VARCHAR(50) NOT NULL UNIQUE,
    service_address_line1 VARCHAR(255),
    service_city VARCHAR(100),
    service_state VARCHAR(2),
    service_zip VARCHAR(10),
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_external_location_id (external_location_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Meter Readings table
CREATE TABLE meter_readings (
    id CHAR(36) PRIMARY KEY,
    meter_id CHAR(36) NOT NULL,
    reading_date DATE NOT NULL,
    usage_ccf DECIMAL(10,2) NOT NULL,
    source ENUM('IMPORT_XLSX', 'IMPORT_CSV', 'API', 'MANUAL') NOT NULL,
    ingested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_payload_hash VARCHAR(64),
    FOREIGN KEY (meter_id) REFERENCES meters(id),
    UNIQUE KEY unique_meter_reading (meter_id, reading_date),
    INDEX idx_meter_date (meter_id, reading_date),
    INDEX idx_reading_date (reading_date),
    INDEX idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rate Plans table
CREATE TABLE rate_plans (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    customer_type_scope ENUM('RESIDENTIAL', 'COMMERCIAL', 'ANY') NOT NULL,
    effective_start_date DATE NOT NULL,
    effective_end_date DATE NULL,
    status ENUM('DRAFT', 'ACTIVE', 'RETIRED') NOT NULL DEFAULT 'DRAFT',
    created_by CHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_customer_type (customer_type_scope),
    INDEX idx_effective_dates (effective_start_date, effective_end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rate Components table
CREATE TABLE rate_components (
    id CHAR(36) PRIMARY KEY,
    rate_plan_id CHAR(36) NOT NULL,
    component_type ENUM('TIERED_USAGE', 'FIXED_FEE', 'SEASONAL_MULTIPLIER', 'SURCHARGE_PERCENT', 'SURCHARGE_FLAT') NOT NULL,
    name VARCHAR(255) NOT NULL,
    config_json JSON NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id) ON DELETE CASCADE,
    INDEX idx_rate_plan (rate_plan_id),
    INDEX idx_component_type (component_type),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Billing Periods table
CREATE TABLE billing_periods (
    id CHAR(36) PRIMARY KEY,
    cycle_number INT NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    status ENUM('OPEN', 'CLOSED', 'BILLED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_cycle_period (cycle_number, period_start_date),
    INDEX idx_cycle_number (cycle_number),
    INDEX idx_period_dates (period_start_date, period_end_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bills table
CREATE TABLE bills (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    meter_id CHAR(36) NOT NULL,
    billing_period_id CHAR(36) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('DRAFT', 'ISSUED', 'SENT', 'PAID', 'VOID') NOT NULL DEFAULT 'DRAFT',
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_fees DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_surcharges DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    delivered_via ENUM('PORTAL', 'EMAIL', 'BOTH') NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (meter_id) REFERENCES meters(id),
    FOREIGN KEY (billing_period_id) REFERENCES billing_periods(id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_billing_period (billing_period_id),
    INDEX idx_status (status),
    INDEX idx_dates (issue_date, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bill Line Items table
CREATE TABLE bill_line_items (
    id CHAR(36) PRIMARY KEY,
    bill_id CHAR(36) NOT NULL,
    line_type ENUM('USAGE_CHARGE', 'FEE', 'SURCHARGE', 'ADJUSTMENT') NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10,2) NULL,
    unit VARCHAR(50) NULL,
    unit_rate DECIMAL(10,4) NULL,
    amount DECIMAL(10,2) NOT NULL,
    rate_component_id CHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
    FOREIGN KEY (rate_component_id) REFERENCES rate_components(id),
    INDEX idx_bill_id (bill_id),
    INDEX idx_line_type (line_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bill Delivery Log table
CREATE TABLE bill_delivery_log (
    id CHAR(36) PRIMARY KEY,
    bill_id CHAR(36) NOT NULL,
    channel ENUM('EMAIL', 'PORTAL_NOTIFICATION') NOT NULL,
    destination VARCHAR(255) NOT NULL,
    status ENUM('QUEUED', 'SENT', 'FAILED') NOT NULL DEFAULT 'QUEUED',
    provider_message_id VARCHAR(255) NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES bills(id),
    INDEX idx_bill_id (bill_id),
    INDEX idx_status (status),
    INDEX idx_channel (channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Usage Forecasts table
CREATE TABLE usage_forecasts (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    meter_id CHAR(36) NOT NULL,
    billing_cycle_number INT NOT NULL,
    target_period_start DATE NOT NULL,
    target_period_end DATE NOT NULL,
    predicted_total_ccf DECIMAL(10,2) NOT NULL,
    predicted_total_amount DECIMAL(10,2) NOT NULL,
    method ENUM('SIMPLE_AVG', 'SEASONAL_AVG', 'TREND_BASED') NOT NULL,
    confidence_level ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL,
    historical_periods_used INT NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (meter_id) REFERENCES meters(id),
    INDEX idx_customer_meter (customer_id, meter_id),
    INDEX idx_target_period (target_period_start, target_period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Anomaly Events table
CREATE TABLE anomaly_events (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    meter_id CHAR(36) NOT NULL,
    event_date DATE NOT NULL,
    event_type ENUM('SPIKE', 'SUSTAINED_HIGH', 'ZERO_USAGE', 'NEGATIVE_USAGE', 'DATA_GAP') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL,
    description TEXT NOT NULL,
    status ENUM('OPEN', 'INVESTIGATING', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'OPEN',
    created_by CHAR(36) NULL,
    resolved_by CHAR(36) NULL,
    resolution_note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (meter_id) REFERENCES meters(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (resolved_by) REFERENCES users(id),
    INDEX idx_customer_meter (customer_id, meter_id),
    INDEX idx_status (status),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_event_date (event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Support Notes table
CREATE TABLE support_notes (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    created_by CHAR(36) NOT NULL,
    note_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit Log table
CREATE TABLE audit_log (
    id CHAR(36) PRIMARY KEY,
    actor_user_id CHAR(36) NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id CHAR(36) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_user_id) REFERENCES users(id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_actor (actor_user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Import Runs table
CREATE TABLE import_runs (
    id CHAR(36) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    source_type ENUM('XLSX', 'CSV', 'API') NOT NULL,
    status ENUM('IN_PROGRESS', 'COMPLETED', 'FAILED') NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    rows_inserted INT NOT NULL DEFAULT 0,
    rows_updated INT NOT NULL DEFAULT 0,
    rows_rejected INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    file_checksum VARCHAR(64),
    started_by CHAR(36),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (started_by) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Import Errors table
CREATE TABLE import_errors (
    id CHAR(36) PRIMARY KEY,
    import_run_id CHAR(36) NOT NULL,
    row_number INT NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    row_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (import_run_id) REFERENCES import_runs(id) ON DELETE CASCADE,
    INDEX idx_import_run (import_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add foreign key constraint for users -> customers
ALTER TABLE users
ADD CONSTRAINT fk_users_customer
FOREIGN KEY (customer_id) REFERENCES customers(id);
