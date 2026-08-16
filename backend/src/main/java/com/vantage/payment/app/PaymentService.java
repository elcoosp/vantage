package com.vantage.payment.app;
import com.vantage.core.exception.IdempotencyConflictException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.tenant.TenantContext;
import com.vantage.payment.domain.IdempotencyKey;
import com.vantage.payment.domain.IdempotencyKeyRepository;
import com.vantage.payment.ui.dto.PaymentRequest;
import com.vantage.payment.ui.dto.PaymentResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final long TTL_HOURS = 24;
    // Sentinel persisted as responseBody while the payment is in flight. Keeps the
    // NOT NULL column satisfied and lets the unique (idempotency_key, tenant_id)
    // constraint act as a distributed reservation lock against concurrent duplicates.
    private static final int STATUS_PENDING = 0;
    private static final String PENDING_BODY = "PENDING";
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

        // Fast path: an already-completed key (existing response) is returned as-is.
        Optional<IdempotencyKey> existing = idempotencyKeyRepository
            .findByIdempotencyKeyAndTenantId(idempotencyKeyHeader, tenantId);
        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            if (key.getResponseBody() != null && !PENDING_BODY.equals(key.getResponseBody())) {
                if (!key.getRequestHash().equals(requestHash)) {
                    log.warn("Idempotency key {} payload mismatch - tampering detected", key.getIdempotencyKey());
                    throw new IdempotencyConflictException(
                        "Payload tampering detected for idempotency key: " + key.getIdempotencyKey());
                }
                log.info("Cache hit for idempotency key {}", key.getIdempotencyKey());
                return deserializeResponse(key.getResponseBody());
            }
            // A concurrent request already reserved this key but hasn't finished.
            return waitForCompletion(key.getIdempotencyKey(), tenantId, requestHash);
        }

        // Reserve the key first. The unique (idempotency_key, tenant_id) constraint
        // guarantees only one thread/instance can win this insert, so the payment
        // below executes at most once per key (no double-charge under concurrency).
        IdempotencyKey reservation = new IdempotencyKey();
        reservation.setIdempotencyKey(idempotencyKeyHeader);
        reservation.setTenantId(tenantId);
        reservation.setRequestHash(requestHash);
        reservation.setResponseStatus(STATUS_PENDING);
        reservation.setResponseBody(PENDING_BODY);
        reservation.setExpiresAt(Instant.now().plusSeconds(TTL_HOURS * 3600));
        try {
            idempotencyKeyRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            // Lost the race: another request is handling this key. Return its result.
            log.info("Idempotency key {} already reserved by another request", idempotencyKeyHeader);
            return waitForCompletion(idempotencyKeyHeader, tenantId, requestHash);
        }

        // We own the reservation: execute the payment exactly once, then persist it.
        PaymentResponse response = executePayment(request);
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            reservation.setResponseStatus(HttpStatus.OK.value());
            reservation.setResponseBody(responseJson);
            idempotencyKeyRepository.save(reservation);
            log.info("Stored idempotency key {} for tenant {}", idempotencyKeyHeader, tenantId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for key {}", idempotencyKeyHeader, e);
            throw new IllegalStateException("Failed to serialize payment response", e);
        }
        return response;
    }

    /**
     * Polls for the completed response of a key reserved by a concurrent request.
     * A duplicate payload returns the shared result; a mismatched payload is a conflict.
     */
    private PaymentResponse waitForCompletion(String idempotencyKey, UUID tenantId, String requestHash) {
        for (int attempt = 0; attempt < 20; attempt++) {
            IdempotencyKey key = idempotencyKeyRepository
                .findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId).orElse(null);
            if (key != null && key.getResponseBody() != null && !PENDING_BODY.equals(key.getResponseBody())) {
                if (!key.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(
                        "Payload tampering detected for idempotency key: " + key.getIdempotencyKey());
                }
                return deserializeResponse(key.getResponseBody());
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Winner did not complete in time; fail safe rather than double-charge.
        throw new IllegalStateException("Idempotency key " + idempotencyKey + " reservation timed out");
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