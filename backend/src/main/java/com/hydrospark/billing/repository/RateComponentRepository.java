package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.RateComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateComponentRepository extends JpaRepository<RateComponent, String> {
    List<RateComponent> findByRatePlanIdAndIsActiveTrueOrderBySortOrderAsc(String ratePlanId);
}
