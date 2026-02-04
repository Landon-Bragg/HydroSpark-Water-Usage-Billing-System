package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, String> {
    
    List<Bill> findByCustomerId(String customerId);
    
    List<Bill> findByCustomerIdOrderByIssueDateDesc(String customerId);
    
    Optional<Bill> findByCustomerIdAndBillingPeriodId(String customerId, String billingPeriodId);
    
    List<Bill> findByBillingPeriodId(String billingPeriodId);
    
    List<Bill> findByStatus(Bill.Status status);
    
    /**
     * Find overdue bills - due date before today and status is unpaid
     */
    @Query("SELECT b FROM Bill b WHERE b.dueDate < :date AND b.status IN :statuses")
    List<Bill> findByDueDateBeforeAndStatusIn(
        @Param("date") LocalDate date,
        @Param("statuses") List<Bill.Status> statuses
    );
    
    /**
     * Find bills by status within a date range
     */
    @Query("SELECT b FROM Bill b WHERE b.issueDate BETWEEN :startDate AND :endDate AND b.status = :status")
    List<Bill> findByIssueDateBetweenAndStatus(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") Bill.Status status
    );
}
