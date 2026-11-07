package com.vantage.core.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Typed base interface for every repository. All repository interfaces extend
 * this (directly or via {@link TenantRepositoryBase}), and
 * {@link TenantJpaRepositoryImpl} implements it.
 *
 * <p>This indirection exists for Spring Modulith: its Spring Data fragment
 * detection calls {@code AbstractRepositoryMetadata.getMetadata(...)} on
 * classes that implement raw {@link JpaRepository}, which fails the
 * "must be an interface" assertion and breaks module documentation. With a
 * typed base interface the fragment resolves cleanly.</p>
 *
 * @param <T>  the entity type
 * @param <ID> the entity identifier type
 */
@NoRepositoryBean
public interface TenantRepositoryBase<T, ID> extends JpaRepository<T, ID> {
}
