package com.delivery.deliveryplataform.catalog.application.dto;

public record DishRequest(
        String name,
        String description,
        Double price,
        Boolean available
) {
}
