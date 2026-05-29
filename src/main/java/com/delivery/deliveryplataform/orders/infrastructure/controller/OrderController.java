package com.delivery.deliveryplataform.orders.infrastructure.controller;

import com.delivery.deliveryplataform.orders.application.dto.CreateOrderRequest;
import com.delivery.deliveryplataform.orders.application.dto.OrderResponse;
import com.delivery.deliveryplataform.orders.application.dto.UpdateOrderStatusRequest;
import com.delivery.deliveryplataform.orders.application.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Crear pedido (CLIENTE)
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, auth.getName()));
    }

    // Listar mis pedidos (filtrado por rol)
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> myOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getMyOrders(auth.getName()));
    }

    // Obtener un pedido específico
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(orderService.getOrderById(id, auth.getName()));
    }

    // Cambiar estado (RESTAURANTE acepta/despacha, REPARTIDOR entrega)
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication auth) {
        return ResponseEntity.ok(orderService.updateStatus(id, request, auth.getName()));
    }

    // Cancelar pedido (solo CLIENTE dueño y solo si está RECIBIDA)
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(orderService.cancelOrder(id, auth.getName()));
    }
}