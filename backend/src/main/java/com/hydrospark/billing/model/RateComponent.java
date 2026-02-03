package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rate_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateComponent {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(name = "rate_plan_id", nullable = false, columnDefinition = "CHAR(36)")
    private String ratePlanId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false)
    private ComponentType componentType;
    
    @Column(nullable = false)
    private String name;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false, columnDefinition = "JSON")
    private String configJson;
    
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    public enum ComponentType {
        TIERED_USAGE,
        FIXED_FEE,
        SEASONAL_MULTIPLIER,
        SURCHARGE_PERCENT,
        SURCHARGE_FLAT
    }
}
