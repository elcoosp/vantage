package com.vantage;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ModulithVerificationIT {

    @Test
    void should_verify_module_boundaries() {
        var modules = ApplicationModules.of(VantageApplication.class);
        Violations violations = modules.detectViolations();

        if (violations.iterator().hasNext()) {
            System.err.println("=== Module Boundary Violations ===");
            violations.forEach(v -> System.err.println("  - " + v.toString()));
            System.err.println("=== End of violations ===");
            fail("Module boundary violations found");
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
