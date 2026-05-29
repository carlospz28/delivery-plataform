package com.delivery.deliveryplataform.orders.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long dishId,
        @NotNull @Min(value = 1, message = "La cantidad debe ser al menos 1") Integer quantity
) {}