package com.delivery.deliveryplataform.auth.application.dto;

import com.delivery.deliveryplataform.auth.domain.model.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String name,
        Role role
) {}