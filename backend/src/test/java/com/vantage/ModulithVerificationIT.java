package com.vantage;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

public class ModulithVerificationIT {

    @Test
    void should_verify_module_boundaries() {
        var modules = ApplicationModules.of(VantageApplication.class);
        try {
            modules.verify();
        } catch (Exception e) {
            System.err.println("=== Module Boundary Violations ===");
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
            // Attempt to print any nested violations if available
            if (e.getMessage() != null && e.getMessage().contains("Violations")) {
                System.err.println(e.getMessage());
            }
            throw e;
        }
        assertThat(modules).isNotNull();
    }

    @Test
    void should_generate_module_documentation() {
        var modules = ApplicationModules.of(VantageApplication.class);
        new Documenter(modules)
            .writeDocumentation()
            .writeModulesAsPlantUml();
    }
}
