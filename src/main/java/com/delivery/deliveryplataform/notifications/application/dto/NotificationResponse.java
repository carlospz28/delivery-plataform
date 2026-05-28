package com.delivery.deliveryplataform.notifications.application.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String message,
        String type,
        Boolean read,
        LocalDateTime createdAt
) {
}
