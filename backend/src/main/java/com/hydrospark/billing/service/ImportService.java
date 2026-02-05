package com.hydrospark.billing.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportService {

    // =========================
    // EXISTING EXCEL IMPORT
    // =========================
    // KEEP your real implementation here.
    // This stub is only to make this file complete and compilable.
    public ImportResult importFromExcel(InputStream in, String originalFilename, String userId) {
        ImportResult result = new ImportResult();
        result.setSuccess(true);
        result.setMessage("Excel import handled elsewhere");
        result.setRowsProcessed(0);
        return result;
    }

    // =========================
    // CSV IMPORT (NEW)
    // =========================
    public ImportResult importFromCsv(InputStream in, String originalFilename, String userId)
            throws IOException {

        int rowsProcessed = 0;

        try (
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            CSVReader csv = new CSVReader(reader)
        ) {
            String[] header = csv.readNext();
            if (header == null) {
                throw new IllegalArgumentException("CSV is empty");
            }

            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                if (header[i] != null) {
                    idx.put(header[i].trim().toLowerCase(Locale.ROOT), i);
                }
            }

            // REQUIRED columns (adjust if needed)
            require(idx, "meterid");
            require(idx, "date");
            require(idx, "usage");

            String[] row;
            while ((row = csv.readNext()) != null) {
                rowsProcessed++;

                String meterId = get(row, idx, "meterid");
                String dateStr = get(row, idx, "date");
                String usageStr = get(row, idx, "usage");

                if (isBlank(meterId) || isBlank(dateStr) || isBlank(usageStr)) {
                    continue;
                }

                LocalDate date = LocalDate.parse(dateStr.trim());
                double usage = Double.parseDouble(usageStr.trim());

                // 🔗 TODO:
                // Call the SAME persistence logic used by Excel imports
                // Example:
                // saveMeterReading(meterId, date, usage, userId);
            }

        } catch (CsvValidationException e) {
            throw new IllegalArgumentException("Invalid CSV format: " + e.getMessage(), e);
        }

        ImportResult result = new ImportResult();
        result.setSuccess(true);
        result.setMessage("CSV upload parsed successfully");
        result.setRowsProcessed(rowsProcessed);
        return result;
    }

    // =========================
    // HELPERS
    // =========================
    private static void require(Map<String, Integer> idx, String col) {
        if (!idx.containsKey(col.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Missing required column: " + col);
        }
    }

    private static String get(String[] row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col.toLowerCase(Locale.ROOT));
        if (i == null || i < 0 || i >= row.length) return null;
        return row[i];
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // =========================
    // RESULT DTO
    // =========================
    @Data
    public static class ImportResult {
        private boolean success;
        private String message;
        private int rowsProcessed;
    }
}
