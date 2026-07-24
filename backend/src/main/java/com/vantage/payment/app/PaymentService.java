package com.vantage.payment.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.tenant.TenantContext;
import com.vantage.payment.domain.IdempotencyKey;
import com.vantage.payment.domain.IdempotencyKeyRepository;
import com.vantage.payment.ui.dto.PaymentRequest;
import com.vantage.payment.ui.dto.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private static final long TTL_HOURS = 24;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse processPayment(String idempotencyKeyHeader, PaymentRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }

        String requestHash = hashRequest(request);

        return idempotencyKeyRepository.findByIdempotencyKeyAndTenantId(idempotencyKeyHeader, tenantId)
            .map(existing -> handleExistingKey(existing, requestHash, request))
            .orElseGet(() -> handleNewKey(idempotencyKeyHeader, tenantId, requestHash, request));
    }

    private PaymentResponse handleExistingKey(IdempotencyKey existing, String requestHash, PaymentRequest request) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("Payload tampering detected for idempotency key: " + existing.getIdempotencyKey());
        }
        return deserializeResponse(existing.getResponseBody());
    }

    private PaymentResponse handleNewKey(String idempotencyKey, UUID tenantId, String requestHash, PaymentRequest request) {
        PaymentResponse response = executePayment(request);
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            IdempotencyKey key = new IdempotencyKey();
            key.setIdempotencyKey(idempotencyKey);
            key.setTenantId(tenantId);
            key.setRequestHash(requestHash);
            key.setResponseStatus(HttpStatus.OK.value());
            key.setResponseBody(responseJson);
            key.setExpiresAt(Instant.now().plusSeconds(TTL_HOURS * 3600));
            idempotencyKeyRepository.save(key);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payment response", e);
        }
        return response;
    }

    private PaymentResponse executePayment(PaymentRequest request) {
        // Mock payment logic: success if amount < 1000, else failure
        boolean success = request.amount().compareTo(new BigDecimal("1000")) < 0;
        UUID transactionId = UUID.randomUUID();
        if (success) {
            return new PaymentResponse(transactionId, "SUCCESS", "Payment processed successfully");
        } else {
            return new PaymentResponse(transactionId, "FAILED", "Insufficient funds");
        }
    }

    private String hashRequest(PaymentRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash request", e);
        }
    }

    private PaymentResponse deserializeResponse(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, PaymentResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached response", e);
        }
    }
}
