package com.vantage.order.query.ui;

import com.vantage.core.tenant.TenantContext;
import com.vantage.order.query.domain.OrderSearchView;
import com.vantage.order.query.domain.OrderSearchViewRepository;
import com.vantage.order.query.ui.dto.OrderSearchPageResponse;
import com.vantage.order.query.ui.dto.OrderSearchResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderQueryController {
    private final OrderSearchViewRepository repository;

    public OrderQueryController(OrderSearchViewRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/search")
    public OrderSearchPageResponse searchOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrderSearchView> orders;
        if (status != null && !status.isBlank()) {
            orders = repository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            orders = repository.findByTenantId(tenantId, pageable);
        }

        Page<OrderSearchResultResponse> responsePage = orders.map(view -> new OrderSearchResultResponse(
            view.getOrderId(),
            view.getProductName(),
            view.getStatus(),
            view.getQuantity(),
            view.getCreatedAt()
        ));

        return OrderSearchPageResponse.from(responsePage);
    }
}
