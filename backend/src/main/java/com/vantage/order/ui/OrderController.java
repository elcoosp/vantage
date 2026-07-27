// backend/src/main/java/com/vantage/order/ui/OrderController.java
package com.vantage.order.ui;

import com.vantage.order.app.OrderService;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.vantage.api.api.ApiApi;
import com.vantage.api.model.OrderRequest;
import com.vantage.api.model.OrderResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController implements ApiApi {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Override
    public ResponseEntity<OrderResponse> apiV1OrdersPost(OrderRequest orderRequest) {
        com.vantage.order.ui.dto.OrderRequest internalRequest =
            new com.vantage.order.ui.dto.OrderRequest(
                orderRequest.getProductId(),
                orderRequest.getQuantity(),
                null
            );
        com.vantage.order.ui.dto.OrderResponse internalResponse = orderService.createOrder(internalRequest);
        OrderResponse response = new OrderResponse()
            .orderId(internalResponse.id())
            .status(OrderResponse.StatusEnum.fromValue(internalResponse.status().name()));
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

}