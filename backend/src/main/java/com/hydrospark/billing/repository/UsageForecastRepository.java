package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.UsageForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UsageForecastRepository extends JpaRepository<UsageForecast, String> {
    
    @Query("SELECT uf FROM UsageForecast uf " +
           "WHERE uf.customerId = :customerId " +
           "AND uf.targetPeriodStart <= :date " +
           "AND uf.targetPeriodEnd >= :date " +
           "ORDER BY uf.generatedAt DESC")
    Optional<UsageForecast> findLatestForecastForCustomerAndDate(
        @Param("customerId") String customerId,
        @Param("date") LocalDate date);
}
