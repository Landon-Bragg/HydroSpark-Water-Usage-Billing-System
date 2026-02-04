-- V2__Seed_Data.sql
-- Initial seed data for HydroSpark system

-- Insert default admin user
-- Password: Admin123!
INSERT INTO users (id, email, password_hash, role, customer_id, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@hydrospark.com',
    '$2a$10$G2OheeEt0dVy2oNAw5mM5OlviRp8gv43Uk2Y4Ip4.Nu63pNABWQUq',
    'ADMIN',
    NULL,
    TRUE
);

-- Insert default billing user
-- Password: Billing123!
INSERT INTO users (id, email, password_hash, role, customer_id, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'billing@hydrospark.com',
    '$2a$10$ASaT5h/1au8jWR0Z0lH7B.49SKdOX.2GK37eozAYnBTLvQFHJbxwK',
    'BILLING',
    NULL,
    TRUE
);

-- Insert default operations user
-- Password: Operations123!
INSERT INTO users (id, email, password_hash, role, customer_id, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    'operations@hydrospark.com',
    '$2a$10$UwuTg8qJeWVw3AxQO7uMruzvVVADbM3qc.TGfdx0aT/5/pXas9ItO',
    'OPERATIONS',
    NULL,
    TRUE
);

-- Insert default support user
-- Password: Support123!
INSERT INTO users (id, email, password_hash, role, customer_id, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000004',
    'support@hydrospark.com',
    '$2a$10$veMcDUCMZ7uWk0unqzgMq.UybWN.xSkxnuMO2OfjqA6RSQgHH5HYm',
    'SUPPORT',
    NULL,
    TRUE
);

-- Insert a demo customer
-- FIXED: Split name into first/last, added account_number, added service address fields
INSERT INTO customers (
    id, 
    account_number, 
    first_name, 
    last_name, 
    phone, 
    email, 
    service_address, 
    city, 
    state, 
    zip_code, 
    mailing_address_line1, 
    mailing_city, 
    mailing_state, 
    mailing_zip, 
    billing_cycle_number, 
    customer_type
)
VALUES (
    '00000000-0000-0000-0000-000000000100',
    'CUST-001', 
    'Demo', 
    'Customer', 
    '555-0100',
    'customer@example.com',
    '123 Main Street', 
    'Dallas', 
    'TX', 
    '75201',
    '123 Main Street',
    'Dallas',
    'TX',
    '75201',
    20,
    'RESIDENTIAL'
);

-- Insert demo customer user
-- Password: Customer123!
INSERT INTO users (id, email, password_hash, role, customer_id, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000005',
    'customer@hydrospark.com',
    '$2a$10$Hrz8H5qYewCPisl/SyGdm.My1hOJm2guGVcUAROFPOneLOEmzt2da',
    'CUSTOMER',
    '00000000-0000-0000-0000-000000000100',
    TRUE
);

-- Insert demo meter
INSERT INTO meters (id, customer_id, external_location_id, service_address_line1, service_city, service_state, service_zip, status)
VALUES (
    '00000000-0000-0000-0000-000000000200',
    '00000000-0000-0000-0000-000000000100',
    'DEMO-METER-001',
    '123 Main Street',
    'Dallas',
    'TX',
    '75201',
    'ACTIVE'
);

-- Create default Residential Rate Plan
INSERT INTO rate_plans (id, name, customer_type_scope, effective_start_date, effective_end_date, status, created_by)
VALUES (
    '00000000-0000-0000-0000-000000000300',
    'Standard Residential Rate',
    'RESIDENTIAL',
    '2018-01-01',
    NULL,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000001'
);

-- Add tiered usage component
-- Tier 1: 0-5 CCF @ $2.50/CCF
-- Tier 2: 5-15 CCF @ $3.25/CCF
-- Tier 3: 15+ CCF @ $4.10/CCF
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000400',
    '00000000-0000-0000-0000-000000000300',
    'TIERED_USAGE',
    'Base Water Usage',
    JSON_OBJECT(
        'unit', 'CCF',
        'tiers', JSON_ARRAY(
            JSON_OBJECT('up_to', 5, 'rate_per_ccf', 2.50),
            JSON_OBJECT('up_to', 15, 'rate_per_ccf', 3.25),
            JSON_OBJECT('up_to', NULL, 'rate_per_ccf', 4.10)
        )
    ),
    1,
    TRUE
);

-- Add summer seasonal multiplier (June-September)
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000401',
    '00000000-0000-0000-0000-000000000300',
    'SEASONAL_MULTIPLIER',
    'Summer Peak Season',
    JSON_OBJECT(
        'applies_months', JSON_ARRAY(6, 7, 8, 9),
        'multiplier', 1.15
    ),
    2,
    TRUE
);

-- Add infrastructure fixed fee
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000402',
    '00000000-0000-0000-0000-000000000300',
    'FIXED_FEE',
    'Infrastructure Maintenance Fee',
    JSON_OBJECT('amount', 12.00),
    3,
    TRUE
);

-- Add environmental surcharge (3% of subtotal)
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000403',
    '00000000-0000-0000-0000-000000000300',
    'SURCHARGE_PERCENT',
    'Environmental Compliance Surcharge',
    JSON_OBJECT(
        'percent', 0.03,
        'applies_to', 'SUBTOTAL'
    ),
    4,
    TRUE
);

-- Create Commercial Rate Plan
INSERT INTO rate_plans (id, name, customer_type_scope, effective_start_date, effective_end_date, status, created_by)
VALUES (
    '00000000-0000-0000-0000-000000000310',
    'Standard Commercial Rate',
    'COMMERCIAL',
    '2018-01-01',
    NULL,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000001'
);

-- Commercial tiered usage
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000410',
    '00000000-0000-0000-0000-000000000310',
    'TIERED_USAGE',
    'Commercial Water Usage',
    JSON_OBJECT(
        'unit', 'CCF',
        'tiers', JSON_ARRAY(
            JSON_OBJECT('up_to', 50, 'rate_per_ccf', 3.00),
            JSON_OBJECT('up_to', 200, 'rate_per_ccf', 3.75),
            JSON_OBJECT('up_to', NULL, 'rate_per_ccf', 4.50)
        )
    ),
    1,
    TRUE
);

-- Commercial fixed fee
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000411',
    '00000000-0000-0000-0000-000000000310',
    'FIXED_FEE',
    'Commercial Service Fee',
    JSON_OBJECT('amount', 35.00),
    2,
    TRUE
);