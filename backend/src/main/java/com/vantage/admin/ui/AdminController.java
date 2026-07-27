package com.vantage.admin.ui;

import com.vantage.core.admin.ChaosMonkeyService;
import com.vantage.admin.ui.dto.ChaosMonkeyToggleRequest;
import com.vantage.admin.ui.dto.SystemMetricsResponse;
import com.vantage.order.domain.OrderRepository;
import com.vantage.vendor.domain.VendorRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/chaos-monkey/payment-failure")
    public void togglePaymentFailure(@RequestBody ChaosMonkeyToggleRequest request) {
        System.err.println("AdminController.togglePaymentFailure called with enabled=" + request.enabled());
        if (request.enabled()) {
            chaosMonkeyService.enablePaymentFailure();
        } else {
            chaosMonkeyService.disablePaymentFailure();
        }
    }

    @GetMapping("/chaos-monkey/payment-failure")
    public boolean getPaymentFailureStatus() {
        boolean status = chaosMonkeyService.isPaymentFailureEnabled();
        System.err.println("AdminController.getPaymentFailureStatus returning " + status);
        return status;
    }

    @GetMapping("/metrics")
    public SystemMetricsResponse getMetrics() {
        long totalVendors = vendorRepository.count();
        long totalOrders = orderRepository.count();
        String circuitBreakerState = circuitBreakerRegistry.find("payment")
                .map(cb -> cb.getState().name())
                .orElse("UNKNOWN");
        return new SystemMetricsResponse(totalVendors, totalOrders, circuitBreakerState);
    }
}
