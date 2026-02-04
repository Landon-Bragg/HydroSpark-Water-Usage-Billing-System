package com.hydrospark.billing.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for seed data
 * Run this class to generate hashes, then update V2__Seed_Data.sql
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=".repeat(80));
        System.out.println("BCrypt Password Hashes for HydroSpark Seed Data");
        System.out.println("=".repeat(80));
        System.out.println();

        // Admin user
        String adminPassword = "Admin123!";
        String adminHash = encoder.encode(adminPassword);
        System.out.println("Admin User:");
        System.out.println("  Email: admin@hydrospark.com");
        System.out.println("  Password: " + adminPassword);
        System.out.println("  Hash: " + adminHash);
        System.out.println();

        // Billing user
        String billingPassword = "Billing123!";
        String billingHash = encoder.encode(billingPassword);
        System.out.println("Billing User:");
        System.out.println("  Email: billing@hydrospark.com");
        System.out.println("  Password: " + billingPassword);
        System.out.println("  Hash: " + billingHash);
        System.out.println();

        // Operations user
        String operationsPassword = "Operations123!";
        String operationsHash = encoder.encode(operationsPassword);
        System.out.println("Operations User:");
        System.out.println("  Email: operations@hydrospark.com");
        System.out.println("  Password: " + operationsPassword);
        System.out.println("  Hash: " + operationsHash);
        System.out.println();

        // Support user
        String supportPassword = "Support123!";
        String supportHash = encoder.encode(supportPassword);
        System.out.println("Support User:");
        System.out.println("  Email: support@hydrospark.com");
        System.out.println("  Password: " + supportPassword);
        System.out.println("  Hash: " + supportHash);
        System.out.println();

        // Customer user
        String customerPassword = "Customer123!";
        String customerHash = encoder.encode(customerPassword);
        System.out.println("Customer User:");
        System.out.println("  Email: customer@hydrospark.com");
        System.out.println("  Password: " + customerPassword);
        System.out.println("  Hash: " + customerHash);
        System.out.println();

        System.out.println("=".repeat(80));
        System.out.println("Update these hashes in: backend/src/main/resources/db/migration/V2__Seed_Data.sql");
        System.out.println("=".repeat(80));
    }
}
