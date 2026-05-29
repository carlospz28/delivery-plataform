package com.delivery.deliveryplataform.orders.application.dto;

public record OrderItemResponse(
        Long id,
        Long dishId,
        String dishName,
        Integer quantity,
        Double unitPrice,
        Double subtotal
) {}