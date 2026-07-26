package com.vantage.core.admin.app;

import org.springframework.stereotype.Service;

@Service
public class ChaosMonkeyService {
    private static final java.util.concurrent.atomic.AtomicBoolean paymentFailureEnabled = new java.util.concurrent.atomic.AtomicBoolean(false);

    public void enablePaymentFailure() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void disablePaymentFailure() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean isPaymentFailureEnabled() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
