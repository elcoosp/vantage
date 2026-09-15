package com.vantage.core.chat.infra;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persisted chat conversation. One conversation = one thread of messages
 * (alternating user / assistant turns with optional tool calls in between).
 * Multi-tenant isolation is enforced by the Hibernate {@code tenantFilter_*}
 * filter defined via {@link FilterDef}/{@link Filter}, matching the pattern
 * used by all other entities in the codebase.
 */
@Entity
@Table(name = "ai_conversations")
@FilterDef(name = "tenantFilter_AiConversation", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter_AiConversation", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class AiConversation extends BaseTenantEntity {

    @Column(name = "title", length = 255)
    private String title;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AiMessage> messages = new ArrayList<>();
}
