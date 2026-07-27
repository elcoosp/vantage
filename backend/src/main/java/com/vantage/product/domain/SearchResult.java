package com.vantage.product.domain;

import java.util.UUID;

public interface SearchResult {
    String getEntityType();
    UUID getId();
    String getTitle();
    String getDescription();
}
