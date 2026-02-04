package com.hydrospark.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for meter data exposed via API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterDTO {
    private String id;

    private String customerId;
    private String externalLocationId;

    private String serviceAddressLine1;
    private String serviceCity;
    private String serviceState;
    private String serviceZip;
}
