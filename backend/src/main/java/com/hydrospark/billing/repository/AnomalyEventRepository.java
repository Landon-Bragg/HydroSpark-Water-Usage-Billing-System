package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, String> {
    
    List<AnomalyEvent> findByCustomerId(String customerId);
    
    List<AnomalyEvent> findByMeterId(String meterId);
    
    List<AnomalyEvent> findByStatus(AnomalyEvent.Status status);
    
    List<AnomalyEvent> findByStatusAndSeverity(AnomalyEvent.Status status, AnomalyEvent.Severity severity);
    
    List<AnomalyEvent> findByStatusOrderByCreatedAtDesc(AnomalyEvent.Status status);
}
