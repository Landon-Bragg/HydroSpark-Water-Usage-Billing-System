package com.hydrospark.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
public class HydroSparkBillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HydroSparkBillingApplication.class, args);
        System.out.println("\n" +
            "=".repeat(70) + "\n" +
            "  HydroSpark Billing System Started Successfully!\n" +
            "=".repeat(70) + "\n" +
            "  Backend API: http://localhost:8080\n" +
            "  Swagger UI:  http://localhost:8080/swagger-ui.html\n" +
            "  API Docs:    http://localhost:8080/v3/api-docs\n" +
            "  Health:      http://localhost:8080/actuator/health\n" +
            "=".repeat(70) + "\n"
        );
    }
}
