package com.vantage.order.query.graphql;

import com.vantage.core.tenant.TenantContext;
import com.vantage.core.tenant.TenantGraphQlInterceptor;
import com.vantage.order.query.domain.OrderSearchView;
import com.vantage.order.query.domain.OrderSearchViewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import graphql.schema.DataFetchingEnvironment;
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
    public List<OrderSearchResult> orders(DataFetchingEnvironment environment,
                                          @Argument String status, @Argument Integer page, @Argument Integer size) {
        UUID tenantId = resolveTenantId(environment);
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

    private UUID resolveTenantId(DataFetchingEnvironment environment) {
        // The tenant is propagated into the GraphQL context by TenantGraphQlInterceptor because the
        // query executes on a thread that does not inherit the servlet request thread-local.
        Object context = environment.getContext();
        if (context instanceof graphql.GraphQLContext graphQLContext) {
            Object fromContext = graphQLContext.get(TenantGraphQlInterceptor.TENANT_KEY);
            if (fromContext instanceof UUID uuid) {
                return uuid;
            }
        }
        return TenantContext.getTenantId();
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
