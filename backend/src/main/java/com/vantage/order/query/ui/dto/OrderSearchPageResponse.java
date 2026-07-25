package com.vantage.order.query.ui.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record OrderSearchPageResponse(
    List<OrderSearchResultResponse> content,
    int totalPages,
    long totalElements,
    int number,
    int size
) {
    public static OrderSearchPageResponse from(Page<OrderSearchResultResponse> page) {
        return new OrderSearchPageResponse(
            page.getContent(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getNumber(),
            page.getSize()
        );
    }
}
