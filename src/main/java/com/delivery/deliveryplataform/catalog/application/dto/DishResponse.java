package com.delivery.deliveryplataform.catalog.application.dto;

import java.time.LocalDateTime;

public record DishResponse(
        Long id,
        Long restaurantId,
        String name,
        String description,
        Double price,
        Boolean available,
        LocalDateTime createdAt
) {
}
