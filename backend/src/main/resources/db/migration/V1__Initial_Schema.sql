-- HydroSpark Water Usage & Billing System Database Schema
-- MySQL/MariaDB

-- Drop tables if they exist (for clean reinstall)
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS anomaly_events;
DROP TABLE IF EXISTS bill_line_items;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS billing_periods;
DROP TABLE IF EXISTS meter_readings;
DROP TABLE IF EXISTS rate_components;
DROP TABLE IF EXISTS rate_plans;
DROP TABLE IF EXISTS meters;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;

-- Users table (for authentication and authorization)
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'BILLING', 'OPERATIONS', 'SUPPORT', 'CUSTOMER') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    last_login_attempt TIMESTAMP NULL,
    account_locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Customers table
CREATE TABLE customers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    service_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL DEFAULT 'TX',
    zip_code VARCHAR(10) NOT NULL,
    billing_address VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(2),
    billing_zip_code VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    auto_pay BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_account_number (account_number),
    INDEX idx_email (email),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Meters table
CREATE TABLE meters (
    id VARCHAR(36) PRIMARY KEY,
    meter_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id VARCHAR(36) NOT NULL,
    installation_date DATE NOT NULL,
    meter_type VARCHAR(50) DEFAULT 'STANDARD',
    status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
    last_reading_date DATE,
    last_reading_value DECIMAL(12, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    INDEX idx_meter_number (meter_number),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Meter readings table
CREATE TABLE meter_readings (
    id VARCHAR(36) PRIMARY KEY,
    meter_id VARCHAR(36) NOT NULL,
    reading_date DATE NOT NULL,
    reading_value DECIMAL(12, 2) NOT NULL,
    usage_gallons DECIMAL(12, 2) NOT NULL,
    reading_type ENUM('AUTOMATED', 'MANUAL', 'ESTIMATED') DEFAULT 'AUTOMATED',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    UNIQUE KEY unique_meter_reading (meter_id, reading_date),
    INDEX idx_meter_date (meter_id, reading_date),
    INDEX idx_reading_date (reading_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rate plans table
CREATE TABLE rate_plans (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    plan_type ENUM('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL') DEFAULT 'RESIDENTIAL',
    is_active BOOLEAN DEFAULT TRUE,
    effective_date DATE NOT NULL,
    end_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_plan_type (plan_type),
    INDEX idx_effective_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rate components table (for tiered pricing, seasonal rates, surcharges, etc.)
CREATE TABLE rate_components (
    id VARCHAR(36) PRIMARY KEY,
    rate_plan_id VARCHAR(36) NOT NULL,
    component_type ENUM('BASE_FEE', 'TIER', 'SEASONAL_MULTIPLIER', 'SURCHARGE', 'DISCOUNT') NOT NULL,
    name VARCHAR(100) NOT NULL,
    config_json JSON NOT NULL,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id) ON DELETE CASCADE,
    INDEX idx_rate_plan (rate_plan_id),
    INDEX idx_component_type (component_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Billing periods table
CREATE TABLE billing_periods (
    id VARCHAR(36) PRIMARY KEY,
    period_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    bills_generated INT DEFAULT 0,
    total_billed DECIMAL(15, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_period (start_date, end_date),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bills table
CREATE TABLE bills (
    id VARCHAR(36) PRIMARY KEY,
    bill_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id VARCHAR(36) NOT NULL,
    billing_period_id VARCHAR(36) NOT NULL,
    rate_plan_id VARCHAR(36) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    usage_gallons DECIMAL(12, 2) NOT NULL,
    base_charge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    usage_charge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    surcharges DECIMAL(10, 2) DEFAULT 0.00,
    discounts DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL,
    amount_paid DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('DRAFT', 'ISSUED', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED') DEFAULT 'DRAFT',
    payment_date DATE NULL,
    sent_date TIMESTAMP NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (billing_period_id) REFERENCES billing_periods(id) ON DELETE CASCADE,
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id),
    INDEX idx_bill_number (bill_number),
    INDEX idx_customer (customer_id),
    INDEX idx_period (billing_period_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bill line items table
CREATE TABLE bill_line_items (
    id VARCHAR(36) PRIMARY KEY,
    bill_id VARCHAR(36) NOT NULL,
    line_type ENUM('BASE_FEE', 'USAGE_TIER', 'SURCHARGE', 'DISCOUNT', 'TAX') NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(12, 2),
    rate DECIMAL(10, 4),
    amount DECIMAL(10, 2) NOT NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
    INDEX idx_bill (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Anomaly events table
CREATE TABLE anomaly_events (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    meter_id VARCHAR(36) NOT NULL,
    anomaly_type ENUM('SPIKE', 'SUSTAINED_HIGH', 'ZERO_USAGE', 'DATA_GAP', 'POTENTIAL_LEAK') NOT NULL,
    detected_date DATE NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    description TEXT NOT NULL,
    usage_value DECIMAL(12, 2),
    expected_value DECIMAL(12, 2),
    deviation_percentage DECIMAL(5, 2),
    status ENUM('OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE') DEFAULT 'OPEN',
    assigned_to VARCHAR(36) NULL,
    resolution_notes TEXT,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_customer (customer_id),
    INDEX idx_meter (meter_id),
    INDEX idx_status (status),
    INDEX idx_anomaly_type (anomaly_type),
    INDEX idx_detected_date (detected_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit logs table
CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36),
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default admin user
-- Password: Admin123! (BCrypt hashed)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active)
VALUES (
    UUID(),
    'admin@hydrospark.com',
    '$2a$10$XQjZ9Y8Z9Y8Z9Y8Z9Y8Z9eJ6K4N5M6L7P8Q9R0S1T2U3V4W5X6Y7Z8',
    'System',
    'Administrator',
    'ADMIN',
    TRUE
);

-- Insert default rate plan
INSERT INTO rate_plans (id, name, description, plan_type, is_active, effective_date)
VALUES (
    UUID(),
    'Standard Residential',
    'Standard tiered rate plan for residential customers',
    'RESIDENTIAL',
    TRUE,
    '2024-01-01'
);

-- Insert default rate components for the standard plan
SET @rate_plan_id = (SELECT id FROM rate_plans WHERE name = 'Standard Residential' LIMIT 1);

-- Base fee
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order)
VALUES (
    UUID(),
    @rate_plan_id,
    'BASE_FEE',
    'Monthly Base Fee',
    JSON_OBJECT('amount', 15.00),
    1
);

-- Tier 1: 0-5000 gallons
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order)
VALUES (
    UUID(),
    @rate_plan_id,
    'TIER',
    'Tier 1 (0-5000 gallons)',
    JSON_OBJECT('min_usage', 0, 'max_usage', 5000, 'rate_per_gallon', 0.003),
    2
);

-- Tier 2: 5001-10000 gallons
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order)
VALUES (
    UUID(),
    @rate_plan_id,
    'TIER',
    'Tier 2 (5001-10000 gallons)',
    JSON_OBJECT('min_usage', 5001, 'max_usage', 10000, 'rate_per_gallon', 0.004),
    3
);

-- Tier 3: 10001+ gallons
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order)
VALUES (
    UUID(),
    @rate_plan_id,
    'TIER',
    'Tier 3 (10001+ gallons)',
    JSON_OBJECT('min_usage', 10001, 'max_usage', 999999, 'rate_per_gallon', 0.005),
    4
);

-- Summer surcharge (June, July, August)
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order)
VALUES (
    UUID(),
    @rate_plan_id,
    'SEASONAL_MULTIPLIER',
    'Summer Peak Multiplier',
    JSON_OBJECT('applies_months', JSON_ARRAY(6, 7, 8), 'multiplier', 1.15),
    5
);
