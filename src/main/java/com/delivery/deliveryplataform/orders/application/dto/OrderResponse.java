package com.delivery.deliveryplataform.orders.application.dto;

import com.delivery.deliveryplataform.orders.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        Long restaurantId,
        String restaurantName,
        Long driverId,         // nullable hasta que se asigne
        String driverName,     // nullable hasta que se asigne
        OrderStatus status,
        Double total,
        String deliveryAddress,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}