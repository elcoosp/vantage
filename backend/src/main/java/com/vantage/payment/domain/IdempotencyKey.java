package com.vantage.payment.domain;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"idempotency_key", "tenant_id"})
})
@Getter
@Setter
public class IdempotencyKey extends BaseTenantEntity {

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}