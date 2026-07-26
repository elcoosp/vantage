package com.vantage.core.audit.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditListenerRegistrar {

    private static final Logger log = LoggerFactory.getLogger(AuditListenerRegistrar.class);

    private final EntityManagerFactory entityManagerFactory;
    private final AuditEntityEventListener listener;

    public AuditListenerRegistrar(EntityManagerFactory entityManagerFactory, AuditEntityEventListener listener) {
        this.entityManagerFactory = entityManagerFactory;
        this.listener = listener;
    }

    @PostConstruct
    public void registerListeners() {
        try {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            if (sessionFactory instanceof SessionFactoryImpl impl) {
                EventListenerRegistry registry = impl.getServiceRegistry().getService(EventListenerRegistry.class);
                registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(listener);
                registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(listener);
                log.info("AuditListenerRegistrar registered listeners via SessionFactoryImpl");
            } else {
                log.warn("AuditListenerRegistrar: SessionFactory is not SessionFactoryImpl, cannot register");
            }
        } catch (Exception e) {
            log.error("Failed to register audit listener", e);
        }
    }
}
