// backend/src/main/java/com/vantage/order/ui/OrderController.java
package com.vantage.order.ui;

import com.vantage.order.app.OrderService;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

/**
 * Order checkout and fulfillment. Does not implement the generated
 * {@code ApiApi} interface — its default method mappings would duplicate the
 * endpoints below and break boot with ambiguous mappings (this is exactly the
 * failure mode the original code shipped with).
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Places an order and kicks off the checkout saga (inventory reservation →
     * payment capture) over RabbitMQ. Returns 202 while processing continues
     * asynchronously; poll GET /api/v1/orders/search (or the audit timeline) to
     * observe CREATED → PAID.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Fulfillment hand-off: PAID → SHIPPED. Emits an OrderShippedEvent that
     * drives the CQRS search projection and the live ops map.
     */
    @PatchMapping("/{orderId}/ship")
    public ResponseEntity<OrderResponse> ship(@PathVariable UUID orderId) {
        OrderResponse response = orderService.shipOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Customer confirmation: SHIPPED → DELIVERED.
     */
    @PatchMapping("/{orderId}/deliver")
    public ResponseEntity<OrderResponse> deliver(@PathVariable UUID orderId) {
        OrderResponse response = orderService.deliverOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an order before payment capture. 409 once the saga has moved
     * the order past CREATED.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID orderId) {
        OrderResponse response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

}
