package com.vantage.order.query.graphql;

import com.vantage.core.tenant.TenantContext;
import com.vantage.order.query.domain.OrderSearchView;
import com.vantage.order.query.domain.OrderSearchViewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class OrderGraphQLController {

    private final OrderSearchViewRepository orderSearchViewRepository;

    public OrderGraphQLController(OrderSearchViewRepository orderSearchViewRepository) {
        this.orderSearchViewRepository = orderSearchViewRepository;
    }

    @QueryMapping
    public List<OrderSearchResult> orders(@Argument String status, @Argument Integer page, @Argument Integer size) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrderSearchView> orders;
        if (status != null && !status.isBlank()) {
            orders = orderSearchViewRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            orders = orderSearchViewRepository.findByTenantId(tenantId, pageable);
        }

        return orders.getContent().stream()
                .map(view -> new OrderSearchResult(
                        view.getOrderId().toString(),
                        view.getProductName(),
                        view.getStatus(),
                        view.getQuantity(),
                        view.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    public record OrderSearchResult(
            String orderId,
            String productName,
            String status,
            Integer quantity,
            String createdAt
    ) {
    }
}
