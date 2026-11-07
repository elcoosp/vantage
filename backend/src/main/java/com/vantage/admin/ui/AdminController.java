package com.vantage.admin.ui;

import com.vantage.core.admin.ChaosMonkeyService;
import com.vantage.admin.ui.dto.ChaosMonkeyToggleRequest;
import com.vantage.admin.ui.dto.SystemMetricsResponse;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.domain.OrderRepository;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Admin operations (chaos monkey, platform metrics, tenant directory).
 *
 * <p>Every endpoint requires the caller's tenant to be flagged
 * {@code is_admin} in the vendors table — previously ANY authenticated vendor
 * could disable the payment gateway failure injection for everyone.</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ChaosMonkeyService chaosMonkeyService;
    private final VendorRepository vendorRepository;
    private final OrderRepository orderRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public AdminController(ChaosMonkeyService chaosMonkeyService,
                           VendorRepository vendorRepository,
                           OrderRepository orderRepository,
                           CircuitBreakerRegistry circuitBreakerRegistry) {
        this.chaosMonkeyService = chaosMonkeyService;
        this.vendorRepository = vendorRepository;
        this.orderRepository = orderRepository;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    private void requireAdmin() {
        UUID tenantId = TenantContext.getTenantId();
        Vendor vendor = tenantId == null ? null : vendorRepository.findByTenantIdWithoutFilter(tenantId).orElse(null);
        if (vendor == null || !vendor.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    @PostMapping("/chaos-monkey/payment-failure")
    public void togglePaymentFailure(@RequestBody ChaosMonkeyToggleRequest request) {
        requireAdmin();
        if (request.enabled()) {
            chaosMonkeyService.enablePaymentFailure();
        } else {
            chaosMonkeyService.disablePaymentFailure();
        }
    }

    @GetMapping("/chaos-monkey/payment-failure")
    public boolean getPaymentFailureStatus() {
        requireAdmin();
        return chaosMonkeyService.isPaymentFailureEnabled();
    }

    @GetMapping("/metrics")
    public SystemMetricsResponse getMetrics() {
        requireAdmin();
        long totalVendors = vendorRepository.count();
        long totalOrders = orderRepository.count();
        String circuitBreakerState = circuitBreakerRegistry.find("payment")
                .map(cb -> cb.getState().name())
                .orElse("UNKNOWN");
        return new SystemMetricsResponse(totalVendors, totalOrders, circuitBreakerState);
    }
}
