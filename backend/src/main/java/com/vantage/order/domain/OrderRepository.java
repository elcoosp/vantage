// backend/src/main/java/com/vantage/order/domain/OrderRepository.java
package com.vantage.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
}
