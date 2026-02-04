package com.hydrospark.billing.controller;

import com.hydrospark.billing.dto.RatePlanDTO;
import com.hydrospark.billing.model.RateComponent;
import com.hydrospark.billing.model.RatePlan;
import com.hydrospark.billing.repository.RateComponentRepository;
import com.hydrospark.billing.repository.RatePlanRepository;
import com.hydrospark.billing.service.RateEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class RateEngineController {

    private final RateEngineService rateEngineService;
    private final RatePlanRepository ratePlanRepository;
    private final RateComponentRepository rateComponentRepository;

    // -----------------------
    // Rate plan CRUD
    // -----------------------

    @GetMapping("/plans")
    public ResponseEntity<List<RatePlanDTO>> listPlans(@RequestParam(value = "status", required = false) String status) {
        List<RatePlan> plans = (status == null)
                ? ratePlanRepository.findAll()
                : ratePlanRepository.findByStatus(RatePlan.Status.valueOf(status));

        return ResponseEntity.ok(plans.stream().map(this::toDTOWithComponents).toList());
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<RatePlanDTO> getPlan(@PathVariable String planId) {
        RatePlan plan = ratePlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Rate plan not found"));
        return ResponseEntity.ok(toDTOWithComponents(plan));
    }

    @PostMapping("/plans")
    public ResponseEntity<RatePlanDTO> createPlan(@RequestBody RatePlanDTO dto) {
        RatePlan plan = new RatePlan();
        applyToEntity(plan, dto);
        plan = ratePlanRepository.save(plan);

        upsertComponents(plan.getId(), dto.getComponents());
        return ResponseEntity.ok(toDTOWithComponents(plan));
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<RatePlanDTO> updatePlan(@PathVariable String planId, @RequestBody RatePlanDTO dto) {
        RatePlan plan = ratePlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Rate plan not found"));

        applyToEntity(plan, dto);
        plan = ratePlanRepository.save(plan);

        upsertComponents(plan.getId(), dto.getComponents());
        return ResponseEntity.ok(toDTOWithComponents(plan));
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable String planId) {
        if (!ratePlanRepository.existsById(planId)) {
            throw new ResponseStatusException(NOT_FOUND, "Rate plan not found");
        }
        // delete components first (no FK cascade guarantees in this mapping)
        rateComponentRepository.findByRatePlanIdAndIsActiveTrueOrderBySortOrderAsc(planId)
                .forEach(rc -> rateComponentRepository.deleteById(rc.getId()));
        ratePlanRepository.deleteById(planId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------
    // Rate calculation
    // -----------------------

    @PostMapping("/calculate")
    public ResponseEntity<RateEngineService.ChargeBreakdown> calculate(@RequestBody CalculateRequest request) {
        RatePlan plan = ratePlanRepository.findById(request.ratePlanId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Rate plan not found"));

        BigDecimal usageCcf = request.usageCcf() == null ? BigDecimal.ZERO : request.usageCcf();
        LocalDate billDate = request.billingDate() == null ? LocalDate.now() : request.billingDate();

        return ResponseEntity.ok(rateEngineService.calculateCharges(usageCcf, plan, billDate));
    }

    // -----------------------
    // Helpers
    // -----------------------

    private RatePlanDTO toDTOWithComponents(RatePlan plan) {
        List<RateComponent> components = rateComponentRepository
                .findByRatePlanIdAndIsActiveTrueOrderBySortOrderAsc(plan.getId());

        List<RatePlanDTO.RateComponentDTO> compDTOs = components.stream()
                .map(c -> RatePlanDTO.RateComponentDTO.builder()
                        .id(c.getId())
                        .ratePlanId(c.getRatePlanId())
                        .componentType(c.getComponentType() != null ? c.getComponentType().name() : null)
                        .name(c.getName())
                        .configJson(c.getConfigJson())
                        .sortOrder(c.getSortOrder())
                        .isActive(c.getIsActive())
                        .build())
                .toList();

        return RatePlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .customerTypeScope(plan.getCustomerTypeScope() != null ? plan.getCustomerTypeScope().name() : null)
                .effectiveStartDate(plan.getEffectiveStartDate())
                .effectiveEndDate(plan.getEffectiveEndDate())
                .status(plan.getStatus() != null ? plan.getStatus().name() : null)
                .components(compDTOs)
                .build();
    }

    private void applyToEntity(RatePlan plan, RatePlanDTO dto) {
        if (dto.getName() != null) plan.setName(dto.getName());
        if (dto.getCustomerTypeScope() != null) plan.setCustomerTypeScope(RatePlan.CustomerTypeScope.valueOf(dto.getCustomerTypeScope()));
        if (dto.getEffectiveStartDate() != null) plan.setEffectiveStartDate(dto.getEffectiveStartDate());
        plan.setEffectiveEndDate(dto.getEffectiveEndDate());
        if (dto.getStatus() != null) plan.setStatus(RatePlan.Status.valueOf(dto.getStatus()));
    }

    private void upsertComponents(String planId, List<RatePlanDTO.RateComponentDTO> components) {
        if (components == null) return;

        for (RatePlanDTO.RateComponentDTO dto : components) {
            RateComponent entity = (dto.getId() != null)
                    ? rateComponentRepository.findById(dto.getId()).orElse(new RateComponent())
                    : new RateComponent();

            entity.setRatePlanId(planId);
            if (dto.getComponentType() != null) entity.setComponentType(RateComponent.ComponentType.valueOf(dto.getComponentType()));
            if (dto.getName() != null) entity.setName(dto.getName());
            if (dto.getConfigJson() != null) entity.setConfigJson(dto.getConfigJson());
            if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
            if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());

            rateComponentRepository.save(entity);
        }
    }

    public record CalculateRequest(String ratePlanId, BigDecimal usageCcf, LocalDate billingDate) {}
}