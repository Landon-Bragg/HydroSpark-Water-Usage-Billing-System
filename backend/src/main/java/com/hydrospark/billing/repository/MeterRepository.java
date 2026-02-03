package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterRepository extends JpaRepository<Meter, String> {
    Optional<Meter> findByExternalLocationId(String externalLocationId);
    List<Meter> findByCustomerId(String customerId);
    List<Meter> findByStatus(Meter.Status status);
}
