package com.vantage.core.chat.infra;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;
import java.util.UUID;

/**
 * A structured tool-call record attached to {@link AiMessage}.
 */
@Entity
@Table(name = "ai_tool_calls")
@FilterDef(name = "tenantFilter_AiToolCall", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter_AiToolCall", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class AiToolCall {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, updatable = false)
    private AiMessage message;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "tool_name", nullable = false, length = 128)
    private String toolName;

    @Column(name = "tool_call_id", length = 128)
    private String toolCallId;

    @Column(name = "arguments", columnDefinition = "TEXT")
    private String arguments;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
