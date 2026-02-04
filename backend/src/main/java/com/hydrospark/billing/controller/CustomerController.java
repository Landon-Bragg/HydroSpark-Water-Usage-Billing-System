package com.hydrospark.billing.controller;

import com.hydrospark.billing.dto.CustomerDTO;
import com.hydrospark.billing.dto.MeterDTO;
import com.hydrospark.billing.model.Customer;
import com.hydrospark.billing.model.Meter;
import com.hydrospark.billing.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> listCustomers(Pageable pageable) {
        Page<CustomerDTO> page = customerService.getAllCustomers(pageable)
                .map(customerService::toDTO);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable String customerId) {
        Customer customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(customerService.toDTO(customer));
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerDTO customerDTO) {
        Customer created = customerService.createCustomer(customerDTO);
        return ResponseEntity.ok(customerService.toDTO(created));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable String customerId,
                                                      @RequestBody CustomerDTO customerDTO) {
        Customer updated = customerService.updateCustomer(customerId, customerDTO);
        return ResponseEntity.ok(customerService.toDTO(updated));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerDTO>> search(@RequestParam("q") String q) {
        List<CustomerDTO> results = customerService.searchCustomers(q)
                .stream()
                .map(customerService::toDTO)
                .toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{customerId}/stats")
    public ResponseEntity<CustomerService.CustomerStatistics> stats(@PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerStatistics(customerId));
    }

    @GetMapping("/{customerId}/meters")
    public ResponseEntity<List<MeterDTO>> getMeters(@PathVariable String customerId) {
        List<MeterDTO> meters = customerService.getCustomerMeters(customerId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(meters);
    }

    @PostMapping("/{customerId}/meters")
    public ResponseEntity<MeterDTO> addMeter(@PathVariable String customerId,
                                             @RequestBody AddMeterRequest request) {
        if (request.externalLocationId() == null || request.externalLocationId().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "externalLocationId is required");
        }
        Meter meter = customerService.addMeter(customerId, request.externalLocationId(), request.serviceAddress());
        return ResponseEntity.ok(toDTO(meter));
    }

    @PostMapping("/meters/{meterId}/deactivate")
    public ResponseEntity<Void> deactivateMeter(@PathVariable String meterId) {
        customerService.deactivateMeter(meterId);
        return ResponseEntity.noContent().build();
    }

    private MeterDTO toDTO(Meter meter) {
        return MeterDTO.builder()
                .id(meter.getId())
                .customerId(meter.getCustomerId())
                .externalLocationId(meter.getExternalLocationId())
                .serviceAddressLine1(meter.getServiceAddressLine1())
                .serviceCity(meter.getServiceCity())
                .serviceState(meter.getServiceState())
                .serviceZip(meter.getServiceZip())
                .build();
    }

    public record AddMeterRequest(String externalLocationId, String serviceAddress) {}
}