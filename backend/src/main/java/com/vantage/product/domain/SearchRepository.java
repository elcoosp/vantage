package com.vantage.product.domain;

import com.vantage.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchRepository extends JpaRepository<Product, UUID> {

    @Query(value = """
        SELECT 'PRODUCT' AS entity_type,
               p.id AS id,
               p.name AS title,
               p.description AS description
        FROM products p
        WHERE p.tenant_id = :tenantId
          AND p.search_vector @@ plainto_tsquery('english', :query)
        UNION
        SELECT 'ORDER' AS entity_type,
               o.id AS id,
               o.status AS title,
               o.status AS description
        FROM orders o
        WHERE o.tenant_id = :tenantId
          AND o.search_vector @@ plainto_tsquery('english', :query)
        """, nativeQuery = true)
    List<SearchResult> search(@Param("query") String query, @Param("tenantId") UUID tenantId);
}
