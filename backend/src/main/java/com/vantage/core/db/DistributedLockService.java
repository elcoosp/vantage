package com.vantage.core.db;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acquires a Postgres advisory lock inside the current transaction so that only one
 * instance of a distributed operation (e.g. the analytics forecast run) executes at a time.
 *
 * <p>Spring Boot 4 dropped {@code @PersistenceContext} injection of the transaction-scoped
 * {@code EntityManager}, so the {@code EntityManagerFactory} is injected instead and a fresh
 * manager is obtained per call. The lock is released automatically when the surrounding
 * transaction commits/rolls back.</p>
 */
@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private final jakarta.persistence.EntityManagerFactory entityManagerFactory;

    public DistributedLockService(jakarta.persistence.EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Transactional
    public boolean tryAcquireLock(String lockName) {
        jakarta.persistence.EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();
            try {
                Query query = entityManager.createNativeQuery(
                    "SELECT pg_try_advisory_xact_lock(hashtext(:lockName), 0)"
                );
                query.setParameter("lockName", lockName);
                Boolean acquired = (Boolean) query.getSingleResult();
                if (Boolean.TRUE.equals(acquired)) {
                    log.info("Acquired advisory lock: {}", lockName);
                } else {
                    log.info("Failed to acquire advisory lock: {}", lockName);
                }
                tx.commit();
                return Boolean.TRUE.equals(acquired);
            } catch (Exception e) {
                tx.rollback();
                log.warn("Exception while trying to acquire lock {}: {}", lockName, e.getMessage());
                return false;
            }
        } finally {
            entityManager.close();
        }
    }

    /**
     * Advisory locks are transaction-scoped: they are released automatically when the
     * surrounding transaction commits or rolls back. This method is retained only for
     * callers that expect the old {@code DistributedLockService.releaseLock} API; it is
     * a no-op.
     */
    public void releaseLock(String lockName) {
        log.debug("releaseLock({}) is a no-op: advisory locks are released on transaction end", lockName);
    }
}