package com.vantage.core.search.app;

import com.vantage.core.search.domain.SearchRepository;
import com.vantage.core.search.domain.SearchResult;
import com.vantage.core.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public List<SearchResult> search(String query) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }
        return searchRepository.search(query, tenantId);
    }
}
