package com.delivery.deliveryplataform.auth.application.dto;

import com.delivery.deliveryplataform.auth.domain.model.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,
        @NotBlank String name,
        String phone,
        @NotNull Role role,
        // Campos opcionales según rol:
        String businessName,    // requerido si role = RESTAURANTE
        String address,         // requerido si role = RESTAURANTE
        String vehicleType,     // requerido si role = REPARTIDOR
        String licensePlate     // requerido si role = REPARTIDOR
) {}