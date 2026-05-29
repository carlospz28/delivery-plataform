package com.delivery.deliveryplataform.orders.application.dto;

import com.delivery.deliveryplataform.orders.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        Long driverId  // requerido solo cuando se pasa a EN_CAMINO
) {}