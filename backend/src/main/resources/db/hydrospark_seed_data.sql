-- HydroSpark Seed Data
-- Initial users, rate plans, and configuration

-- ==============================================
-- USERS (Staff & Test Accounts)
-- ==============================================
-- Password hashes are for bcrypt with the passwords shown in comments
-- All passwords are in format: <Role>123!

-- Admin Account
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active) VALUES
('admin-001', 'admin@hydrospark.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZQNWC3QDFE/d8lj3Z6G', 'Admin', 'User', 'ADMIN', TRUE);

-- Billing Staff
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active) VALUES
('billing-001', 'billing@hydrospark.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZQNWC3QDFE/d8lj3Z6G', 'Billing', 'Manager', 'BILLING', TRUE);

-- Operations Staff
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active) VALUES
('ops-001', 'operations@hydrospark.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZQNWC3QDFE/d8lj3Z6G', 'Operations', 'Manager', 'OPERATIONS', TRUE);

-- Support Staff
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active) VALUES
('support-001', 'support@hydrospark.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZQNWC3QDFE/d8lj3Z6G', 'Support', 'Representative', 'SUPPORT', TRUE);

-- Test Customer Account
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active) VALUES
('customer-001', 'customer@hydrospark.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZQNWC3QDFE/d8lj3Z6G', 'Test', 'Customer', 'CUSTOMER', TRUE);

-- ==============================================
-- RATE PLANS
-- ==============================================

-- Residential Base Rate Plan
INSERT INTO rate_plans (id, plan_name, description, customer_type_scope, effective_date, status) VALUES
('rate-res-001', 'Residential Standard 2024', 'Standard residential water rates for Texas', 'RESIDENTIAL', '2024-01-01', 'ACTIVE');

-- Residential Rate Components
INSERT INTO rate_components (id, rate_plan_id, component_name, component_type, rate_amount, tier_min_usage, tier_max_usage, display_order) VALUES
-- Base charge
('rate-comp-001', 'rate-res-001', 'Base Service Charge', 'BASE_CHARGE', 15.00, NULL, NULL, 1),

-- Tiered usage rates (per CCF - hundred cubic feet)
('rate-comp-002', 'rate-res-001', 'Tier 1: 0-10 CCF', 'USAGE_TIER', 2.50, 0, 10, 2),
('rate-comp-003', 'rate-res-001', 'Tier 2: 11-25 CCF', 'USAGE_TIER', 3.25, 11, 25, 3),
('rate-comp-004', 'rate-res-001', 'Tier 3: 26-50 CCF', 'USAGE_TIER', 4.00, 26, 50, 4),
('rate-comp-005', 'rate-res-001', 'Tier 4: 51+ CCF', 'USAGE_TIER', 5.50, 51, 999999, 5),

-- Summer surcharge (June-September)
('rate-comp-006', 'rate-res-001', 'Summer Conservation Surcharge', 'SEASONAL_SURCHARGE', 1.00, NULL, NULL, 6),

-- Wastewater fee
('rate-comp-007', 'rate-res-001', 'Wastewater Service Fee', 'SERVICE_FEE', 12.00, NULL, NULL, 7),

-- Sales tax
('rate-comp-008', 'rate-res-001', 'Sales Tax (8.25%)', 'TAX', 0.0825, NULL, NULL, 8);

-- Update summer surcharge with season months
UPDATE rate_components SET season_start_month = 6, season_end_month = 9 
WHERE id = 'rate-comp-006';

-- Commercial Base Rate Plan
INSERT INTO rate_plans (id, plan_name, description, customer_type_scope, effective_date, status) VALUES
('rate-com-001', 'Commercial Standard 2024', 'Standard commercial water rates for Texas', 'COMMERCIAL', '2024-01-01', 'ACTIVE');

-- Commercial Rate Components
INSERT INTO rate_components (id, rate_plan_id, component_name, component_type, rate_amount, tier_min_usage, tier_max_usage, display_order) VALUES
-- Base charge
('rate-comp-101', 'rate-com-001', 'Commercial Base Charge', 'BASE_CHARGE', 50.00, NULL, NULL, 1),

-- Tiered usage rates
('rate-comp-102', 'rate-com-001', 'Tier 1: 0-50 CCF', 'USAGE_TIER', 3.00, 0, 50, 2),
('rate-comp-103', 'rate-com-001', 'Tier 2: 51-200 CCF', 'USAGE_TIER', 3.75, 51, 200, 3),
('rate-comp-104', 'rate-com-001', 'Tier 3: 201+ CCF', 'USAGE_TIER', 4.25, 201, 999999, 4),

-- Summer surcharge
('rate-comp-105', 'rate-com-001', 'Summer Peak Demand Surcharge', 'SEASONAL_SURCHARGE', 1.50, NULL, NULL, 5),

-- Wastewater
('rate-comp-106', 'rate-com-001', 'Commercial Wastewater Fee', 'SERVICE_FEE', 35.00, NULL, NULL, 6),

-- Tax
('rate-comp-107', 'rate-com-001', 'Sales Tax (8.25%)', 'TAX', 0.0825, NULL, NULL, 7);

-- Update commercial summer surcharge with season
UPDATE rate_components SET season_start_month = 6, season_end_month = 9 
WHERE id = 'rate-comp-105';

-- Industrial Base Rate Plan
INSERT INTO rate_plans (id, plan_name, description, customer_type_scope, effective_date, status) VALUES
('rate-ind-001', 'Industrial Standard 2024', 'Standard industrial water rates for Texas', 'INDUSTRIAL', '2024-01-01', 'ACTIVE');

-- Industrial Rate Components
INSERT INTO rate_components (id, rate_plan_id, component_name, component_type, rate_amount, tier_min_usage, tier_max_usage, display_order) VALUES
-- Base charge
('rate-comp-201', 'rate-ind-001', 'Industrial Base Charge', 'BASE_CHARGE', 150.00, NULL, NULL, 1),

-- Flat usage rate (volume pricing)
('rate-comp-202', 'rate-ind-001', 'Volume Rate (per CCF)', 'USAGE_TIER', 2.75, 0, 999999, 2),

-- Demand charge
('rate-comp-203', 'rate-ind-001', 'Peak Demand Charge', 'SERVICE_FEE', 75.00, NULL, NULL, 3),

-- Wastewater treatment
('rate-comp-204', 'rate-ind-001', 'Industrial Wastewater Treatment', 'SERVICE_FEE', 100.00, NULL, NULL, 4),

-- Tax
('rate-comp-205', 'rate-ind-001', 'Sales Tax (8.25%)', 'TAX', 0.0825, NULL, NULL, 5);

-- ==============================================
-- BILLING PERIODS (2024-2025)
-- ==============================================

-- 2024 Monthly Billing Periods
INSERT INTO billing_periods (id, period_name, cycle_number, period_start_date, period_end_date, bill_generation_date, bill_due_date, status) VALUES
('period-2024-01', 'January 2024', 1, '2024-01-01', '2024-01-31', '2024-02-01', '2024-02-15', 'COMPLETED'),
('period-2024-02', 'February 2024', 1, '2024-02-01', '2024-02-29', '2024-03-01', '2024-03-15', 'COMPLETED'),
('period-2024-03', 'March 2024', 1, '2024-03-01', '2024-03-31', '2024-04-01', '2024-04-15', 'COMPLETED'),
('period-2024-04', 'April 2024', 1, '2024-04-01', '2024-04-30', '2024-05-01', '2024-05-15', 'COMPLETED'),
('period-2024-05', 'May 2024', 1, '2024-05-01', '2024-05-31', '2024-06-01', '2024-06-15', 'COMPLETED'),
('period-2024-06', 'June 2024', 1, '2024-06-01', '2024-06-30', '2024-07-01', '2024-07-15', 'COMPLETED'),
('period-2024-07', 'July 2024', 1, '2024-07-01', '2024-07-31', '2024-08-01', '2024-08-15', 'COMPLETED'),
('period-2024-08', 'August 2024', 1, '2024-08-01', '2024-08-31', '2024-09-01', '2024-09-15', 'COMPLETED'),
('period-2024-09', 'September 2024', 1, '2024-09-01', '2024-09-30', '2024-10-01', '2024-10-15', 'COMPLETED'),
('period-2024-10', 'October 2024', 1, '2024-10-01', '2024-10-31', '2024-11-01', '2024-11-15', 'COMPLETED'),
('period-2024-11', 'November 2024', 1, '2024-11-01', '2024-11-30', '2024-12-01', '2024-12-15', 'COMPLETED'),
('period-2024-12', 'December 2024', 1, '2024-12-01', '2024-12-31', '2025-01-01', '2025-01-15', 'COMPLETED');

-- 2025 Billing Periods
INSERT INTO billing_periods (id, period_name, cycle_number, period_start_date, period_end_date, bill_generation_date, bill_due_date, status) VALUES
('period-2025-01', 'January 2025', 1, '2025-01-01', '2025-01-31', '2025-02-01', '2025-02-15', 'COMPLETED'),
('period-2025-02', 'February 2025', 1, '2025-02-01', '2025-02-28', '2025-03-01', '2025-03-15', 'OPEN'),
('period-2025-03', 'March 2025', 1, '2025-03-01', '2025-03-31', '2025-04-01', '2025-04-15', 'OPEN');

-- ==============================================
-- STORED PROCEDURES & FUNCTIONS
-- ==============================================

DELIMITER //

-- Function to calculate bill for a customer for a billing period
CREATE PROCEDURE calculate_customer_bill(
    IN p_customer_id VARCHAR(36),
    IN p_billing_period_id VARCHAR(36),
    IN p_rate_plan_id VARCHAR(36)
)
BEGIN
    DECLARE v_total_usage DECIMAL(12,2);
    DECLARE v_subtotal DECIMAL(10,2) DEFAULT 0;
    DECLARE v_tax_amount DECIMAL(10,2) DEFAULT 0;
    DECLARE v_total_amount DECIMAL(10,2);
    DECLARE v_bill_id VARCHAR(36);
    DECLARE v_bill_number VARCHAR(50);
    DECLARE v_period_start DATE;
    DECLARE v_period_end DATE;
    DECLARE v_bill_date DATE;
    DECLARE v_due_date DATE;
    
    -- Get billing period details
    SELECT period_start_date, period_end_date, bill_generation_date, bill_due_date
    INTO v_period_start, v_period_end, v_bill_date, v_due_date
    FROM billing_periods
    WHERE id = p_billing_period_id;
    
    -- Calculate total usage for the period
    SELECT COALESCE(SUM(daily_usage_ccf), 0)
    INTO v_total_usage
    FROM daily_water_readings
    WHERE customer_id = p_customer_id
    AND reading_date BETWEEN v_period_start AND v_period_end;
    
    -- Generate bill ID and number
    SET v_bill_id = UUID();
    SET v_bill_number = CONCAT('BILL-', DATE_FORMAT(NOW(), '%Y%m%d'), '-', SUBSTRING(v_bill_id, 1, 8));
    
    -- Create bill
    INSERT INTO bills (
        id, customer_id, billing_period_id, bill_number,
        bill_date, due_date, total_usage, status
    ) VALUES (
        v_bill_id, p_customer_id, p_billing_period_id, v_bill_number,
        v_bill_date, v_due_date, v_total_usage, 'DRAFT'
    );
    
    -- This is a simplified version - full implementation would calculate
    -- line items based on rate components, tiers, etc.
    
    SELECT v_bill_id AS bill_id, v_total_usage AS total_usage;
END //

-- Procedure to detect usage anomalies
CREATE PROCEDURE detect_usage_anomalies(
    IN p_customer_id VARCHAR(36),
    IN p_detection_date DATE
)
BEGIN
    DECLARE v_actual_usage DECIMAL(12,2);
    DECLARE v_avg_usage DECIMAL(12,2);
    DECLARE v_std_dev DECIMAL(12,2);
    DECLARE v_threshold DECIMAL(12,2);
    DECLARE v_deviation DECIMAL(5,2);
    DECLARE v_meter_id VARCHAR(36);
    
    -- Get meter for customer
    SELECT id INTO v_meter_id
    FROM meters
    WHERE customer_id = p_customer_id
    AND status = 'ACTIVE'
    LIMIT 1;
    
    -- Get actual usage for the date
    SELECT daily_usage_ccf INTO v_actual_usage
    FROM daily_water_readings
    WHERE customer_id = p_customer_id
    AND reading_date = p_detection_date;
    
    -- Calculate average usage for previous 30 days
    SELECT AVG(daily_usage_ccf), STDDEV(daily_usage_ccf)
    INTO v_avg_usage, v_std_dev
    FROM daily_water_readings
    WHERE customer_id = p_customer_id
    AND reading_date BETWEEN DATE_SUB(p_detection_date, INTERVAL 30 DAY) AND DATE_SUB(p_detection_date, INTERVAL 1 DAY);
    
    -- Set threshold at 2 standard deviations
    SET v_threshold = v_avg_usage + (2 * v_std_dev);
    
    -- Check if anomaly detected
    IF v_actual_usage > v_threshold THEN
        SET v_deviation = ((v_actual_usage - v_avg_usage) / v_avg_usage) * 100;
        
        INSERT INTO anomaly_events (
            id, customer_id, meter_id, event_type, severity,
            detected_date, actual_usage, expected_usage, deviation_percentage,
            status, detection_method
        ) VALUES (
            UUID(), p_customer_id, v_meter_id, 'HIGH_USAGE', 
            IF(v_deviation > 200, 'CRITICAL', IF(v_deviation > 100, 'HIGH', 'MEDIUM')),
            p_detection_date, v_actual_usage, v_avg_usage, v_deviation,
            'OPEN', 'STATISTICAL_THRESHOLD'
        );
    END IF;
    
    -- Check for zero usage (possible meter issue)
    IF v_actual_usage = 0 AND v_avg_usage > 1 THEN
        INSERT INTO anomaly_events (
            id, customer_id, meter_id, event_type, severity,
            detected_date, actual_usage, expected_usage, deviation_percentage,
            status, detection_method
        ) VALUES (
            UUID(), p_customer_id, v_meter_id, 'ZERO_USAGE', 'MEDIUM',
            p_detection_date, v_actual_usage, v_avg_usage, -100,
            'OPEN', 'ZERO_USAGE_CHECK'
        );
    END IF;
END //

DELIMITER ;

-- ==============================================
-- VIEWS FOR REPORTING
-- ==============================================

-- Customer Usage Summary View
CREATE OR REPLACE VIEW vw_customer_usage_summary AS
SELECT 
    c.id AS customer_id,
    c.account_number,
    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
    c.customer_type,
    m.id AS meter_id,
    m.external_location_id,
    COUNT(DISTINCT dwr.reading_date) AS total_reading_days,
    SUM(dwr.daily_usage_ccf) AS total_usage_ccf,
    AVG(dwr.daily_usage_ccf) AS avg_daily_usage_ccf,
    MIN(dwr.reading_date) AS first_reading_date,
    MAX(dwr.reading_date) AS last_reading_date
FROM customers c
JOIN meters m ON m.customer_id = c.id
LEFT JOIN daily_water_readings dwr ON dwr.meter_id = m.id
GROUP BY c.id, c.account_number, c.first_name, c.last_name, c.customer_type, m.id, m.external_location_id;

-- Monthly Usage Summary View
CREATE OR REPLACE VIEW vw_monthly_usage_summary AS
SELECT 
    c.id AS customer_id,
    c.account_number,
    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
    m.id AS meter_id,
    dwr.reading_year,
    dwr.reading_month,
    DATE_FORMAT(CONCAT(dwr.reading_year, '-', LPAD(dwr.reading_month, 2, '0'), '-01'), '%Y-%m-01') AS month_start,
    COUNT(*) AS days_in_month,
    SUM(dwr.daily_usage_ccf) AS total_usage_ccf,
    AVG(dwr.daily_usage_ccf) AS avg_daily_usage_ccf,
    MIN(dwr.daily_usage_ccf) AS min_daily_usage_ccf,
    MAX(dwr.daily_usage_ccf) AS max_daily_usage_ccf
FROM customers c
JOIN meters m ON m.customer_id = c.id
JOIN daily_water_readings dwr ON dwr.meter_id = m.id
GROUP BY 
    c.id, c.account_number, c.first_name, c.last_name, 
    m.id, dwr.reading_year, dwr.reading_month;

-- Active Anomalies View
CREATE OR REPLACE VIEW vw_active_anomalies AS
SELECT 
    ae.id,
    ae.detected_date,
    c.account_number,
    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
    c.customer_type,
    ae.event_type,
    ae.severity,
    ae.actual_usage,
    ae.expected_usage,
    ae.deviation_percentage,
    ae.status,
    ae.customer_notified,
    DATEDIFF(CURRENT_DATE, ae.detected_date) AS days_open
FROM anomaly_events ae
JOIN customers c ON c.id = ae.customer_id
WHERE ae.status IN ('OPEN', 'INVESTIGATING')
ORDER BY ae.severity DESC, ae.detected_date DESC;

-- Outstanding Bills View
CREATE OR REPLACE VIEW vw_outstanding_bills AS
SELECT 
    b.id AS bill_id,
    b.bill_number,
    b.bill_date,
    b.due_date,
    c.account_number,
    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
    c.email,
    c.phone,
    bp.period_name,
    b.total_usage,
    b.total_amount,
    b.amount_paid,
    b.balance_due,
    b.status,
    DATEDIFF(CURRENT_DATE, b.due_date) AS days_overdue,
    CASE 
        WHEN b.status = 'PAID' THEN 'Paid'
        WHEN CURRENT_DATE > b.due_date THEN 'Overdue'
        WHEN DATEDIFF(b.due_date, CURRENT_DATE) <= 7 THEN 'Due Soon'
        ELSE 'Current'
    END AS payment_status
FROM bills b
JOIN customers c ON c.id = b.customer_id
JOIN billing_periods bp ON bp.id = b.billing_period_id
WHERE b.status IN ('SENT', 'OVERDUE')
ORDER BY b.due_date;

COMMIT;
