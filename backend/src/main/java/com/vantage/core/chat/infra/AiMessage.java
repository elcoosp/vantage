package com.vantage.core.chat.infra;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single message within an {@link AiConversation}. May represent a user
 * turn, an assistant turn, a system instruction, or a tool result.
 * Tenant isolation is enforced by the Hibernate {@code tenantFilter_AiMessage} filter.
 */
@Entity
@Table(name = "ai_messages")
@FilterDef(name = "tenantFilter_AiMessage", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter_AiMessage", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class AiMessage {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private AiConversation conversation;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AiMessageRole role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_call_id", length = 128)
    private String toolCallId;

    @Column(name = "tool_name", length = 128)
    private String toolName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AiToolCall> toolCalls = new ArrayList<>();

    public enum AiMessageRole {
        USER, ASSISTANT, SYSTEM, TOOL
    }
}
