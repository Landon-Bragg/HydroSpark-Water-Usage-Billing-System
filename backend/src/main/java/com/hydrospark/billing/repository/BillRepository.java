package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, String> {
    
    List<Bill> findByCustomerId(String customerId);
    
    List<Bill> findByCustomerIdOrderByIssueDateDesc(String customerId);
    
    Optional<Bill> findByCustomerIdAndBillingPeriodId(String customerId, String billingPeriodId);
    
    List<Bill> findByBillingPeriodId(String billingPeriodId);
    
    List<Bill> findByStatus(Bill.Status status);
}
