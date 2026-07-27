package com.vantage.product.app;

import com.vantage.product.domain.SearchRepository;
import com.vantage.product.domain.SearchResult;
import com.vantage.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public List<SearchResult> search(String query) {
        log.info("Searching for query: {}", query);
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }
        List<SearchResult> results = searchRepository.search(query, tenantId);
        log.info("Found {} results for query: {}", results.size(), query);
        return results;
    }
}
