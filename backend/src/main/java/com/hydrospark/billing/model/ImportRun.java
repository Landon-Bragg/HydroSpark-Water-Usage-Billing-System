package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportRun {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(nullable = false)
    private String filename;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;
    
    @Column(name = "rows_inserted", nullable = false)
    private Integer rowsInserted = 0;
    
    @Column(name = "rows_updated", nullable = false)
    private Integer rowsUpdated = 0;
    
    @Column(name = "rows_rejected", nullable = false)
    private Integer rowsRejected = 0;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "file_checksum")
    private String fileChecksum;
    
    @Column(name = "started_by", columnDefinition = "CHAR(36)")
    private String startedBy;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
    
    public enum SourceType {
        XLSX, CSV, API
    }
    
    public enum Status {
        IN_PROGRESS, COMPLETED, FAILED
    }
}
