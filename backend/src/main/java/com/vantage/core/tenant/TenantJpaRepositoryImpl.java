package com.vantage.core.tenant;

import com.vantage.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-aware base repository.
 *
 * <p>Hibernate {@code @Filter} is applied to HQL/JPQL/Criteria queries but is NOT applied to
 * {@code EntityManager.find()}/{@code getById()} (root-entity by-id loads). To guarantee
 * cross-tenant isolation for every access path, the by-id lookups are overridden here with an
 * explicit tenant predicate (only for entities that actually carry a {@code tenantId}).
 * Derived queries and {@code findAll()} still rely on the {@code @Filter} (enabled per-session
 * by {@link TenantFilterInterceptor}).</p>
 *
 * @param <T>  the entity type
 * @param <ID> the entity identifier type
 */
public class TenantJpaRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements JpaRepository<T, ID> {

    private final EntityManager entityManager;
    private final Class<T> domainClass;
    private final boolean hasTenantId;

    public TenantJpaRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
        this.hasTenantId = computeHasTenantId(this.domainClass);
    }

    public TenantJpaRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
        this.domainClass = domainClass;
        this.hasTenantId = computeHasTenantId(domainClass);
    }

    private static boolean computeHasTenantId(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field f = current.getDeclaredField("tenantId");
                return f != null;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return false;
    }

    @Override
    public Optional<T> findById(ID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null || !hasTenantId) {
            return super.findById(id);
        }
        String jpql = "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e.id = :id AND e.tenantId = :tenantId";
        try {
            T result = entityManager.createQuery(jpql, domainClass)
                    .setParameter("id", id)
                    .setParameter("tenantId", tenantId)
                    .getSingleResult();
            return Optional.ofNullable(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(ID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null || !hasTenantId) {
            return super.existsById(id);
        }
        String jpql = "SELECT COUNT(e) FROM " + domainClass.getSimpleName()
                + " e WHERE e.id = :id AND e.tenantId = :tenantId";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public T getById(ID id) {
        return findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "No " + domainClass.getSimpleName() + " with id " + id + " for current tenant"));
    }

    @Override
    public T getReferenceById(ID id) {
        return getById(id);
    }
}
