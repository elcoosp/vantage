package com.vantage.core.chat;

import com.vantage.AbstractIntegrationTest;
import com.vantage.core.chat.app.ChatOrchestrator;
import com.vantage.core.chat.app.LlmStreamEvent;
import com.vantage.core.chat.infra.AiConversation;
import com.vantage.core.chat.infra.ConversationRepository;
import com.vantage.core.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that conversation data is isolated per tenant:
 * tenant B cannot see tenant A's conversations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatTenantIsolationIT extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        baseProperties(registry);
        registry.add("vantage.ai.provider", () -> "stub");
    }

    @Autowired private ChatOrchestrator chatOrchestrator;
    @Autowired private ConversationRepository conversationRepository;

    @Test
    void should_not_leak_conversations_between_tenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // Tenant A creates a conversation
        TenantContext.setTenantId(tenantA);
        try {
            chatOrchestrator.stream("Hello from tenant A", null);
        } finally {
            TenantContext.clear();
        }

        // Tenant B creates a conversation
        TenantContext.setTenantId(tenantB);
        try {
            chatOrchestrator.stream("Hello from tenant B", null);
        } finally {
            TenantContext.clear();
        }

        // Tenant A should only see their own conversation
        TenantContext.setTenantId(tenantA);
        try {
            List<AiConversation> aConvs = conversationRepository.findByTenant(tenantA);
            assertThat(aConvs).hasSize(1);
            // Verify the query returns only tenant A's data
            assertThat(aConvs).allMatch(c -> c.getTenantId().equals(tenantA));
        } finally {
            TenantContext.clear();
        }

        // Tenant B should only see their own conversation
        TenantContext.setTenantId(tenantB);
        try {
            List<AiConversation> bConvs = conversationRepository.findByTenant(tenantB);
            assertThat(bConvs).hasSize(1);
            assertThat(bConvs).allMatch(c -> c.getTenantId().equals(tenantB));
        } finally {
            TenantContext.clear();
        }
    }
}
