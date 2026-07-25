package com.vantage.core.search.ui;

import com.vantage.core.search.app.SearchService;
import com.vantage.core.search.domain.SearchResult;
import com.vantage.core.search.ui.dto.SearchResultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<SearchResultResponse> search(@RequestParam("q") String query) {
        List<SearchResult> results = searchService.search(query);
        return results.stream()
                .map(r -> new SearchResultResponse(r.getEntityType(), r.getId(), r.getTitle(), r.getDescription()))
                .toList();
    }
}
