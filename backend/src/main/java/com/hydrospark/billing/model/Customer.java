package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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
