package com.vantage.core.admin.ui;

import com.vantage.core.admin.ui.dto.ChaosMonkeyToggleRequest;
import com.vantage.core.admin.ui.dto.SystemMetricsResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @PostMapping("/chaos-monkey/payment-failure")
    public void togglePaymentFailure(@RequestBody ChaosMonkeyToggleRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @GetMapping("/chaos-monkey/payment-failure")
    public boolean getPaymentFailureStatus() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @GetMapping("/metrics")
    public SystemMetricsResponse getMetrics() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
