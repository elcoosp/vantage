package com.vantage.core.chat.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guardrails for the chat assistant:
 * - PII redaction
 * - Prompt-injection detection
 * - Per-tenant rate limiting (token budget + request budget)
 * - System-prompt composition with tenant context
 */
@Service
public class AiGuardrails {

    @Value("${vantage.ai.max-tokens-per-tenant:40000}")
    private int maxTokensPerTenant;

    @Value("${vantage.ai.max-requests-per-tenant:100}")
    private int maxRequestsPerTenant;

    // Simple PII patterns — email, credit-card-like, phone
    private static final List<Pattern> PII_PATTERNS = Arrays.asList(
        Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}"),
        Pattern.compile("\\d{3}[-.]?\\d{3}[-.]?\\d{4}")
    );

    // Known injection patterns
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile(
            "ignore\\s+(all|previous|earlier|prior).*(instructions|directives|prompts|rules)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile(
            "disregard\\s+(all|previous|earlier|prior).*(instructions|directives|prompts|rules)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile(
            "forget\\s+(all|previous|earlier|prior).*(instructions|directives|prompts|rules)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile(
            "(system\\s*prompt|developer\\s*instruction|you\\s*are\\s*no\\s*longer)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile(
            "\\b(DAN|jailbreak)\\b",
            Pattern.CASE_INSENSITIVE)
    );

    public String redactPii(String text) {
        String redacted = text;
        for (Pattern p : PII_PATTERNS) {
            redacted = p.matcher(redacted).replaceAll("[REDACTED]");
        }
        return redacted;
    }

    public boolean detectPromptInjection(String text) {
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public String buildSystemPrompt(UUID tenantId) {
        return """
            You are Vantage, the support assistant for tenant %s.
            You help the merchant operator with orders, inventory, product search,
            and demand forecasts. Never reveal data from other tenants.
            If you do not know the answer, say so and offer to look it up with a tool.
            Redact any PII you encounter in tool results before surfacing it to the user.
            """.formatted(tenantId);
    }

    public int getMaxTokensPerTenant() {
        return maxTokensPerTenant;
    }

    public int getMaxRequestsPerTenant() {
        return maxRequestsPerTenant;
    }
}
