package com.hydrospark.billing.controller;

import com.hydrospark.billing.model.UsageForecast;
import com.hydrospark.billing.service.ForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    /**
     * Generate a new forecast for a customer (creates and persists a new UsageForecast).
     */
    @PostMapping("/{customerId}/generate")
    public ResponseEntity<UsageForecast> generate(@PathVariable String customerId) {
        return ResponseEntity.ok(forecastService.generateForecast(customerId));
    }

    /**
     * Get the latest forecast for a customer, if one exists.
     */
    @GetMapping("/{customerId}/latest")
    public ResponseEntity<UsageForecast> latest(@PathVariable String customerId) {
        return ResponseEntity.ok(forecastService.getLatestForecast(customerId));
    }
}