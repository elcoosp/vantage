package com.vantage.core.chat.infra;

import com.vantage.core.tenant.TenantContext;
import com.vantage.core.tenant.MissingTenantContextException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class ConversationRepository {

    private final EntityManager entityManager;

    public ConversationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public AiConversation create(String title) {
        UUID tenantId = requireTenant();
        AiConversation conv = new AiConversation();
        conv.setTenantId(tenantId);
        conv.setTitle(title);
        entityManager.persist(conv);
        return conv;
    }

    @Transactional(readOnly = true)
    public AiConversation findById(UUID conversationId) {
        UUID tenantId = requireTenant();
        return entityManager.find(AiConversation.class, conversationId);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<AiConversation> findByTenant(UUID tenantId) {
        var results = entityManager.createNativeQuery(
                "SELECT c.id, c.title, c.created_at, c.updated_at FROM ai_conversations c WHERE c.tenant_id = :tenantId ORDER BY c.created_at DESC")
            .setParameter("tenantId", tenantId)
            .getResultList();
        List<AiConversation> convs = new java.util.ArrayList<>();
        for (Object row : results) {
            Object[] r = (Object[]) row;
            AiConversation conv = new AiConversation();
            conv.setId((UUID) r[0]);
            conv.setTitle((String) r[1]);
            conv.setCreatedAt(r[2] != null ? toInstant(r[2]) : null);
            conv.setUpdatedAt(r[3] != null ? toInstant(r[3]) : null);
            conv.setTenantId(tenantId);
            convs.add(conv);
        }
        return convs;
    }

    @Transactional
    public void delete(UUID conversationId) {
        UUID tenantId = requireTenant();
        entityManager.createNativeQuery("DELETE FROM ai_messages WHERE conversation_id = :convId AND tenant_id = :tenantId")
            .setParameter("convId", conversationId)
            .setParameter("tenantId", tenantId)
            .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM ai_conversations WHERE id = :convId AND tenant_id = :tenantId")
            .setParameter("convId", conversationId)
            .setParameter("tenantId", tenantId)
            .executeUpdate();
    }

    private UUID requireTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException("No tenant context for conversation repository");
        }
        return tenantId;
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant i) return i;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (value instanceof java.util.Date d) return d.toInstant();
        return null;
    }
}
