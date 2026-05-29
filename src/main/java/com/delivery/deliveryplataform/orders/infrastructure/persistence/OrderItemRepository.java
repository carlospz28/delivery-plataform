package com.delivery.deliveryplataform.orders.infrastructure.persistence;

import com.delivery.deliveryplataform.orders.domain.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}