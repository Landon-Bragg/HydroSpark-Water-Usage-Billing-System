package com.hydrospark.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO used for creating/updating customers.
 * (CustomerService expects classic getters like getName(), getCustomerType(), etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private String id;

    private String name;

    /** String representation of Customer.CustomerType enum (e.g., RESIDENTIAL, COMMERCIAL). */
    private String customerType;

    private String phone;
    private String email;

    /** Maps to Customer.mailingAddressLine1 in the entity. */
    private String mailingAddress;

    private String mailingCity;
    private String mailingState;
    private String mailingZip;

    private Integer billingCycleNumber;
    private LocalDateTime createdAt;

}
