package com.vantage.core.admin;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChaosMonkeyService {
    private final AtomicBoolean paymentFailureEnabled = new AtomicBoolean(false);

    public void enablePaymentFailure() {
        System.err.println("ChaosMonkeyService.enablePaymentFailure called");
        paymentFailureEnabled.set(true);
    }

    public void disablePaymentFailure() {
        System.err.println("ChaosMonkeyService.disablePaymentFailure called");
        paymentFailureEnabled.set(false);
    }

    public boolean isPaymentFailureEnabled() {
        boolean result = paymentFailureEnabled.get();
        System.err.println("ChaosMonkeyService.isPaymentFailureEnabled returning " + result);
        return result;
    }
}
