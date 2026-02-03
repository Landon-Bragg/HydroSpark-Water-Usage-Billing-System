package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.BillingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingPeriodRepository extends JpaRepository<BillingPeriod, String> {
    
    Optional<BillingPeriod> findByCycleNumberAndPeriodStartDate(Integer cycleNumber, LocalDate periodStartDate);
    
    List<BillingPeriod> findByCycleNumber(Integer cycleNumber);
    
    List<BillingPeriod> findByStatus(BillingPeriod.Status status);
    
    List<BillingPeriod> findByPeriodStartDateBetween(LocalDate startDate, LocalDate endDate);
}
