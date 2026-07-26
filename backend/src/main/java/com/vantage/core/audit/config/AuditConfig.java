package com.vantage.core.audit.config;

import com.vantage.core.audit.domain.EntityEventRepository;
import com.vantage.core.audit.infrastructure.AuditHelper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    private final EntityEventRepository entityEventRepository;

    public AuditConfig(EntityEventRepository entityEventRepository) {
        this.entityEventRepository = entityEventRepository;
    }

    @PostConstruct
    public void init() {
        AuditHelper.setEntityEventRepository(entityEventRepository);
        System.out.println("*** AuditHelper initialized with repository ***");
    }
}
