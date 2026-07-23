package com.vantage.core.security;

import com.vantage.core.tenant.TenantContext;
import com.vantage.vendor.app.VendorService;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class TenantIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void should_block_cross_tenant_access_when_querying_vendor() {
        AuthResponse tenantA = vendorService.register(new VendorRegistrationRequest("a@vantage.com", "pass", "A"));
        vendorService.register(new VendorRegistrationRequest("b@vantage.com", "pass", "B"));

        UUID vendorAId = vendorRepository.findByEmail("a@vantage.com").orElseThrow().getId();

        TenantContext.setTenantId(tenantA.tenantId());
        try {
            Optional<Vendor> found = vendorRepository.findById(vendorAId);
            assertThat(found).isPresent();
        } finally {
            TenantContext.clear();
        }

        AuthResponse tenantB = vendorService.register(new VendorRegistrationRequest("b2@vantage.com", "pass", "B2"));
        TenantContext.setTenantId(tenantB.tenantId());
        try {
            Optional<Vendor> found = vendorRepository.findById(vendorAId);
            assertThat(found).isEmpty();
        } finally {
            TenantContext.clear();
        }
    }
}
