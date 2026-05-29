package com.delivery.deliveryplataform.orders.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Long restaurantId,
        @NotBlank String deliveryAddress,
        @NotEmpty(message = "El pedido debe tener al menos un platillo")
        @Valid List<OrderItemRequest> items
) {}