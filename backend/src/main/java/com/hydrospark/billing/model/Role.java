package com.hydrospark.billing.model;

/**
 * User roles in the HydroSpark system
 */
public enum Role {
    /**
     * Administrator - Full system access
     */
    ADMIN,

    /**
     * Billing staff - Can generate bills, manage billing periods
     */
    BILLING,

    /**
     * Operations staff - Can manage meters, run anomaly detection
     */
    OPERATIONS,

    /**
     * Support staff - Can view customer data, help with issues
     */
    SUPPORT,

    /**
     * Customer - Can view their own bills and usage
     */
    CUSTOMER
}
