package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "bill_line_items")
@Data
public class BillLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;
    
    @Column(nullable = false)
    private String description;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal rate;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    private Integer lineOrder;
}
