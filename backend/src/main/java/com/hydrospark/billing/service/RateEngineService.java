package com.hydrospark.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hydrospark.billing.model.RateComponent;
import com.hydrospark.billing.model.RatePlan;
import com.hydrospark.billing.repository.RateComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateEngineService {

    private final RateComponentRepository rateComponentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calculate total charges for a given usage amount using the specified rate plan
     */
    public ChargeBreakdown calculateCharges(BigDecimal usageCcf, RatePlan ratePlan, LocalDate billingDate) {
        log.info("Calculating charges for {} CCF using rate plan: {}", usageCcf, ratePlan.getName());

        ChargeBreakdown breakdown = new ChargeBreakdown();
        breakdown.setUsageCcf(usageCcf);
        breakdown.setRatePlanName(ratePlan.getName());

        // Get all active components for this rate plan
        List<RateComponent> components = rateComponentRepository
                .findByRatePlanIdAndIsActiveTrueOrderBySortOrderAsc(ratePlan.getId());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal usageCharge = BigDecimal.ZERO;
        BigDecimal baseFee = BigDecimal.ZERO;
        BigDecimal totalSurcharges = BigDecimal.ZERO;

        // Process each component
        for (RateComponent component : components) {
            try {
                JsonNode config = objectMapper.readTree(component.getConfigJson());

                switch (component.getComponentType()) {
                    case TIERED_USAGE:
                        BigDecimal tierCharge = calculateTieredUsage(usageCcf, config);
                        usageCharge = usageCharge.add(tierCharge);
                        breakdown.addLineItem(component.getName(), tierCharge, "Usage charge");
                        log.debug("Tiered usage charge: ${}", tierCharge);
                        break;

                    case FIXED_FEE:
                        BigDecimal fee = calculateFixedFee(config);
                        baseFee = baseFee.add(fee);
                        breakdown.addLineItem(component.getName(), fee, "Fixed fee");
                        log.debug("Fixed fee: ${}", fee);
                        break;

                    case SEASONAL_MULTIPLIER:
                        BigDecimal multiplier = calculateSeasonalMultiplier(config, billingDate);
                        if (multiplier.compareTo(BigDecimal.ONE) > 0) {
                            BigDecimal seasonalCharge = usageCharge.multiply(multiplier.subtract(BigDecimal.ONE));
                            usageCharge = usageCharge.add(seasonalCharge);
                            breakdown.addLineItem(component.getName(), seasonalCharge, 
                                    "Seasonal adjustment (" + multiplier + "x)");
                            log.debug("Seasonal multiplier: {} = ${}", multiplier, seasonalCharge);
                        }
                        break;

                    case SURCHARGE_PERCENT:
                        subtotal = usageCharge.add(baseFee);
                        BigDecimal percentSurcharge = calculatePercentSurcharge(subtotal, config);
                        totalSurcharges = totalSurcharges.add(percentSurcharge);
                        breakdown.addLineItem(component.getName(), percentSurcharge, "Surcharge");
                        log.debug("Percent surcharge: ${}", percentSurcharge);
                        break;

                    case SURCHARGE_FLAT:
                        BigDecimal flatSurcharge = calculateFlatSurcharge(config);
                        totalSurcharges = totalSurcharges.add(flatSurcharge);
                        breakdown.addLineItem(component.getName(), flatSurcharge, "Surcharge");
                        log.debug("Flat surcharge: ${}", flatSurcharge);
                        break;

                    default:
                        log.warn("Unknown component type: {}", component.getComponentType());
                }

            } catch (Exception e) {
                log.error("Error processing component {}: {}", component.getName(), e.getMessage(), e);
                throw new RuntimeException("Error calculating charges: " + e.getMessage());
            }
        }

        // Calculate final amounts
        breakdown.setUsageCharge(usageCharge);
        breakdown.setBaseFee(baseFee);
        breakdown.setTotalSurcharges(totalSurcharges);
        breakdown.setSubtotal(usageCharge.add(baseFee));
        breakdown.setTotalAmount(usageCharge.add(baseFee).add(totalSurcharges));

        log.info("Charge calculation complete. Total: ${}", breakdown.getTotalAmount());

        return breakdown;
    }

    private BigDecimal calculateTieredUsage(BigDecimal usageCcf, JsonNode config) {
        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal remainingUsage = usageCcf;

        JsonNode tiers = config.get("tiers");
        if (tiers == null || !tiers.isArray()) {
            throw new RuntimeException("Invalid tiered usage configuration");
        }

        for (JsonNode tier : tiers) {
            if (remainingUsage.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal upTo = tier.has("up_to") && !tier.get("up_to").isNull() 
                    ? BigDecimal.valueOf(tier.get("up_to").asDouble())
                    : null; // Unlimited tier

            BigDecimal rateCcf = BigDecimal.valueOf(tier.get("rate_per_ccf").asDouble());

            BigDecimal tierUsage;
            if (upTo == null) {
                // Last tier - unlimited
                tierUsage = remainingUsage;
            } else {
                // Calculate usage for this tier
                BigDecimal tierMax = upTo;
                
                // Find the lower bound (previous tier's upTo value or 0)
                BigDecimal tierMin = BigDecimal.ZERO;
                for (JsonNode prevTier : tiers) {
                    if (prevTier == tier) break;
                    if (prevTier.has("up_to") && !prevTier.get("up_to").isNull()) {
                        tierMin = BigDecimal.valueOf(prevTier.get("up_to").asDouble());
                    }
                }

                BigDecimal tierCapacity = tierMax.subtract(tierMin);
                tierUsage = remainingUsage.min(tierCapacity);
            }

            BigDecimal tierCharge = tierUsage.multiply(rateCcf);
            totalCharge = totalCharge.add(tierCharge);
            remainingUsage = remainingUsage.subtract(tierUsage);

            log.debug("Tier charge: {} CCF @ ${}/CCF = ${}", tierUsage, rateCcf, tierCharge);
        }

        return totalCharge.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFixedFee(JsonNode config) {
        if (!config.has("amount")) {
            throw new RuntimeException("Fixed fee missing 'amount' field");
        }

        return BigDecimal.valueOf(config.get("amount").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSeasonalMultiplier(JsonNode config, LocalDate billingDate) {
        if (!config.has("applies_months") || !config.has("multiplier")) {
            throw new RuntimeException("Seasonal multiplier missing required fields");
        }

        int currentMonth = billingDate.getMonthValue();
        JsonNode appliesMonths = config.get("applies_months");

        // Check if current month is in the applicable months
        boolean applies = false;
        for (JsonNode month : appliesMonths) {
            if (month.asInt() == currentMonth) {
                applies = true;
                break;
            }
        }

        if (applies) {
            return BigDecimal.valueOf(config.get("multiplier").asDouble());
        }

        return BigDecimal.ONE; // No multiplier
    }

    private BigDecimal calculatePercentSurcharge(BigDecimal baseAmount, JsonNode config) {
        if (!config.has("percent")) {
            throw new RuntimeException("Percent surcharge missing 'percent' field");
        }

        BigDecimal percent = BigDecimal.valueOf(config.get("percent").asDouble());
        return baseAmount.multiply(percent).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFlatSurcharge(JsonNode config) {
        if (!config.has("amount")) {
            throw new RuntimeException("Flat surcharge missing 'amount' field");
        }

        return BigDecimal.valueOf(config.get("amount").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Inner class to hold charge breakdown details
     */
    public static class ChargeBreakdown {
        private BigDecimal usageCcf;
        private String ratePlanName;
        private BigDecimal usageCharge = BigDecimal.ZERO;
        private BigDecimal baseFee = BigDecimal.ZERO;
        private BigDecimal totalSurcharges = BigDecimal.ZERO;
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private List<LineItem> lineItems = new ArrayList<>();

        public void addLineItem(String description, BigDecimal amount, String category) {
            lineItems.add(new LineItem(description, amount, category));
        }

        // Getters and setters
        public BigDecimal getUsageCcf() { return usageCcf; }
        public void setUsageCcf(BigDecimal usageCcf) { this.usageCcf = usageCcf; }
        
        public String getRatePlanName() { return ratePlanName; }
        public void setRatePlanName(String ratePlanName) { this.ratePlanName = ratePlanName; }
        
        public BigDecimal getUsageCharge() { return usageCharge; }
        public void setUsageCharge(BigDecimal usageCharge) { this.usageCharge = usageCharge; }
        
        public BigDecimal getBaseFee() { return baseFee; }
        public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }
        
        public BigDecimal getTotalSurcharges() { return totalSurcharges; }
        public void setTotalSurcharges(BigDecimal totalSurcharges) { this.totalSurcharges = totalSurcharges; }
        
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public List<LineItem> getLineItems() { return lineItems; }
        public void setLineItems(List<LineItem> lineItems) { this.lineItems = lineItems; }

        public static class LineItem {
            private String description;
            private BigDecimal amount;
            private String category;

            public LineItem(String description, BigDecimal amount, String category) {
                this.description = description;
                this.amount = amount;
                this.category = category;
            }

            public String getDescription() { return description; }
            public BigDecimal getAmount() { return amount; }
            public String getCategory() { return category; }
        }
    }
}
