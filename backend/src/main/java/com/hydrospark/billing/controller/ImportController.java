package com.hydrospark.billing.controller;

import com.hydrospark.billing.model.ImportRun;
import com.hydrospark.billing.model.User;
import com.hydrospark.billing.repository.ImportRunRepository;
import com.hydrospark.billing.service.AuthService;
import com.hydrospark.billing.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final AuthService authService;
    private final ImportRunRepository importRunRepository;

    /**
     * Upload an Excel file (XLSX) that contains a "DailyUsage" sheet.
     */
    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportService.ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }

        User currentUser = authService.getCurrentUser();
        String userId = currentUser != null ? currentUser.getId() : null;

        try (InputStream in = file.getInputStream()) {
            ImportService.ImportResult result =
                    importService.importFromExcel(in, file.getOriginalFilename(), userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Import failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience endpoint to view recent import runs.
     */
    @GetMapping("/runs/recent")
    public ResponseEntity<List<ImportRun>> recentRuns(@RequestParam(defaultValue = "20") int limit) {
        List<ImportRun> runs = importRunRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .limit(Math.max(1, limit))
                .toList();
        return ResponseEntity.ok(runs);
    }
}