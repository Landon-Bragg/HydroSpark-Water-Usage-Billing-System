package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.ImportRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportRunRepository extends JpaRepository<ImportRun, String> {
    List<ImportRun> findByOrderByStartedAtDesc();
    List<ImportRun> findByStatus(ImportRun.Status status);
}
