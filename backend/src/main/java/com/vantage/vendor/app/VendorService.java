package com.vantage.vendor.app;

import com.vantage.core.security.JwtService;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.vantage.core.exception.ResourceNotFoundException;

@Service
public class VendorService {

    private static final Logger log = LoggerFactory.getLogger(VendorService.class);

    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public VendorService(VendorRepository vendorRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public VendorRegistrationResult register(VendorRegistrationRequest request) {
        UUID tenantId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId);
        vendor.setEmail(request.email());
        vendor.setPasswordHash(passwordEncoder.encode(request.password()));
        vendor.setCompanyName(request.name());

        vendorRepository.save(vendor);
        log.info("Registered new vendor with tenant ID: {}", tenantId);

        String token = jwtService.generateToken(tenantId);
        return new VendorRegistrationResult(tenantId, token);
    }

    @Transactional
    public String updateWebhookUrl(UUID tenantId, String webhookUrl) {
        Vendor vendor = vendorRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for tenant: " + tenantId));
        vendor.setWebhookUrl(webhookUrl);
        String secret = UUID.randomUUID().toString();
        vendor.setWebhookSecret(secret);
        vendorRepository.save(vendor);
        log.info("Updated webhook URL for tenant {}", tenantId);
        return secret;
    }

    /**
     * Returns the webhook configuration for a tenant. The signing secret is
     * masked — its plaintext is only ever shown once, in the PUT response.
     */
    @Transactional(readOnly = true)
    public WebhookConfig getWebhookConfig(UUID tenantId) {
        Vendor vendor = vendorRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for tenant: " + tenantId));
        String url = vendor.getWebhookUrl() == null ? "" : vendor.getWebhookUrl();
        String masked = maskSecret(vendor.getWebhookSecret());
        return new WebhookConfig(url, masked);
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        if (secret.length() <= 8) {
            return secret.charAt(0) + "…" + secret.charAt(secret.length() - 1);
        }
        return secret.substring(0, 4) + "…" + secret.substring(secret.length() - 4);
    }

    public record WebhookConfig(String webhookUrl, String secret) {
    }
}