package com.vantage.core.chat.infra;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Token-usage record for a single LLM call, used for cost tracking and
 * rate-limiting per tenant.
 */
@Entity
@Table(name = "ai_usage")
@FilterDef(name = "tenantFilter_AiUsage", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter_AiUsage", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class AiUsage {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "cost_usd", columnDefinition = "DECIMAL(10,4)")
    private BigDecimal costUsd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
