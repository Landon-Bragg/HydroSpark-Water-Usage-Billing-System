package com.hydrospark.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for rate plans.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatePlanDTO {
    private String id;

    private String name;

    /** String representation of RatePlan.CustomerTypeScope enum. */
    private String customerTypeScope;

    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;

    /** String representation of RatePlan.Status enum. */
    private String status;

    private List<RateComponentDTO> components;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RateComponentDTO {
        private String id;
        private String ratePlanId;

        /** String representation of RateComponent.ComponentType enum. */
        private String componentType;

        private String name;

        /** JSON configuration stored in the DB (RateComponent.configJson). */
        private String configJson;

        private Integer sortOrder;
        private Boolean isActive;
    }
}
