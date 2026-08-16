package com.vantage.core.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.admin.ui.dto.StatusUpdateRequest;
import com.vantage.core.admin.ui.dto.TenantResponse;
import com.vantage.core.security.JwtService;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import com.vantage.vendor.domain.VendorStatus;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AdminTenantManagementIT.TestSecurityConfig.class)
@Testcontainers
public class AdminTenantManagementIT {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(1)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.primary.username", postgres::getUsername);
        registry.add("spring.datasource.primary.password", postgres::getPassword);
        registry.add("spring.datasource.replica.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.replica.username", postgres::getUsername);
        registry.add("spring.datasource.replica.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false"); // enable migrations
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "CORRELATED");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private UUID adminTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UUID vendorTenantId;

    @BeforeEach
    void setup() {
        // Ensure admin exists (seeded by migration)
        // Generate admin token
        // Ensure admin exists in DB (since flyway is disabled)
        // Note: JDBC seeding is used as a test workaround; in production, migration handles this.
        jdbcTemplate.execute(
            "INSERT INTO vendors (id, tenant_id, email, password_hash, company_name, status, is_admin, created_at, updated_at) " +
            "SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'admin@vantage.com', '$2a$10$dummyhashforadmin', 'Vantage Admin', 'ACTIVE', TRUE, NOW(), NOW() " +
            "WHERE NOT EXISTS (SELECT 1 FROM vendors WHERE tenant_id = '11111111-1111-1111-1111-111111111111')"
        );
        adminToken = jwtService.generateToken(adminTenantId);

        // Register a regular vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "vendor-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vendor Store");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
                "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        vendorTenantId = vendorRes.getBody().tenantId();
    }

    @Test
    void should_return_all_tenants_when_admin_authenticated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("X-Tenant-ID", adminTenantId.toString());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<TenantResponse[]> response = restTemplate.exchange(
                "/api/v1/admin/tenants",
                HttpMethod.GET,
                entity,
                TenantResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<TenantResponse> tenants = List.of(response.getBody());
        assertThat(tenants).isNotEmpty();
        // Should include the admin (by tenantSlug) and the registered vendor
        assertThat(tenants).anyMatch(t -> t.tenantSlug().equals(adminTenantId.toString()));
        assertThat(tenants).anyMatch(t -> t.tenantSlug().equals(vendorTenantId.toString()));
    }

    @Test
    void should_suspend_and_reactivate_tenant_when_admin() {
        // Suspend vendor
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", adminTenantId.toString());

        StatusUpdateRequest suspendRequest = new StatusUpdateRequest(VendorStatus.SUSPENDED);
        HttpEntity<StatusUpdateRequest> suspendEntity = new HttpEntity<>(suspendRequest, headers);
        ResponseEntity<Void> suspendRes = restTemplate.exchange(
                "/api/v1/admin/tenants/" + vendorTenantId + "/status",
                HttpMethod.PUT,
                suspendEntity,
                Void.class);
        assertThat(suspendRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify status updated
        Vendor vendor = vendorRepository.findByTenantIdWithoutFilter(vendorTenantId).orElseThrow();
        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.SUSPENDED);

        // Reactivate
        StatusUpdateRequest reactivateRequest = new StatusUpdateRequest(VendorStatus.ACTIVE);
        HttpEntity<StatusUpdateRequest> reactivateEntity = new HttpEntity<>(reactivateRequest, headers);
        ResponseEntity<Void> reactivateRes = restTemplate.exchange(
                "/api/v1/admin/tenants/" + vendorTenantId + "/status",
                HttpMethod.PUT,
                reactivateEntity,
                Void.class);
        assertThat(reactivateRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        vendor = vendorRepository.findByTenantId(vendorTenantId).orElseThrow();
        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.ACTIVE);
    }

    @Test
    void should_return_403_when_suspended_tenant_tries_to_access_protected_endpoint() {
        // 1. Register a vendor and get token
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "suspension-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Suspension Store");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
                "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String vendorToken = vendorRes.getBody().token();
        UUID vendorTenantId = vendorRes.getBody().tenantId();

        // 2. Suspend the vendor via admin
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        adminHeaders.set("X-Tenant-ID", adminTenantId.toString());
        StatusUpdateRequest suspendRequest = new StatusUpdateRequest(VendorStatus.SUSPENDED);
        HttpEntity<StatusUpdateRequest> suspendEntity = new HttpEntity<>(suspendRequest, adminHeaders);
        ResponseEntity<Void> suspendRes = restTemplate.exchange(
                "/api/v1/admin/tenants/" + vendorTenantId + "/status",
                HttpMethod.PUT,
                suspendEntity,
                Void.class);
        assertThat(suspendRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 3. Attempt to access a protected endpoint with vendor token
        HttpHeaders vendorAuthHeaders = new HttpHeaders();
        vendorAuthHeaders.setBearerAuth(vendorToken);
        vendorAuthHeaders.set("X-Tenant-ID", vendorTenantId.toString());
        HttpEntity<Void> entity = new HttpEntity<>(vendorAuthHeaders);

        // Try to get products (or any protected endpoint)
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/products",
                HttpMethod.GET,
                entity,
                String.class);

        // 4. Expect 403 Forbidden
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("Tenant account is suspended");
    }

}