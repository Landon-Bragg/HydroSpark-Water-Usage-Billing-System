package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.RatePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, String> {
    
    List<RatePlan> findByStatus(RatePlan.Status status);
    
    @Query("SELECT rp FROM RatePlan rp " +
           "WHERE rp.status = 'ACTIVE' " +
           "AND rp.effectiveStartDate <= :date " +
           "AND (rp.effectiveEndDate IS NULL OR rp.effectiveEndDate >= :date) " +
           "AND (rp.customerTypeScope = :customerType OR rp.customerTypeScope = 'ANY')")
    Optional<RatePlan> findActiveRatePlanForCustomerType(
        @Param("customerType") RatePlan.CustomerTypeScope customerType,
        @Param("date") LocalDate date);
}
