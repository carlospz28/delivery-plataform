package com.delivery.deliveryplataform.orders.infrastructure.persistence;

import com.delivery.deliveryplataform.orders.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);       // pedidos de un cliente
    List<Order> findByRestaurantId(Long restaurantId);   // pedidos para un restaurante
    List<Order> findByDriverId(Long driverId);           // pedidos asignados a un repartidor
}