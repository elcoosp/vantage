// backend/src/main/java/com/vantage/core/db/DistributedLockService.java
package com.vantage.core.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public boolean tryAcquireLock(String lockName) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT pg_try_advisory_lock(hashtext(:lockName), 0)"
            );
            query.setParameter("lockName", lockName);
            Boolean acquired = (Boolean) query.getSingleResult();
            if (Boolean.TRUE.equals(acquired)) {
                log.info("Acquired advisory lock: {}", lockName);
            } else {
                log.info("Failed to acquire advisory lock: {}", lockName);
            }
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Exception while trying to acquire lock {}: {}", lockName, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void releaseLock(String lockName) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT pg_advisory_unlock(hashtext(:lockName), 0)"
            );
            query.setParameter("lockName", lockName);
            Boolean released = (Boolean) query.getSingleResult();
            if (Boolean.TRUE.equals(released)) {
                log.info("Released advisory lock: {}", lockName);
            } else {
                log.warn("Failed to release advisory lock: {} (may not be held)", lockName);
            }
        } catch (Exception e) {
            log.warn("Exception while releasing lock {}: {}", lockName, e.getMessage());
        }
    }
}
