package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, String> {
    
    Optional<MeterReading> findByMeterIdAndReadingDate(String meterId, LocalDate readingDate);
    
    List<MeterReading> findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(
        String meterId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(mr.usageCcf) FROM MeterReading mr " +
           "WHERE mr.meterId = :meterId " +
           "AND mr.readingDate BETWEEN :startDate AND :endDate")
    BigDecimal sumUsageByMeterAndDateRange(
        @Param("meterId") String meterId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT AVG(mr.usageCcf) FROM MeterReading mr " +
           "WHERE mr.meterId = :meterId " +
           "AND mr.readingDate BETWEEN :startDate AND :endDate")
    Double avgUsageByMeterAndDateRange(
        @Param("meterId") String meterId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(mr) FROM MeterReading mr " +
           "WHERE mr.meterId = :meterId " +
           "AND mr.readingDate BETWEEN :startDate AND :endDate")
    Long countReadingsByMeterAndDateRange(
        @Param("meterId") String meterId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
