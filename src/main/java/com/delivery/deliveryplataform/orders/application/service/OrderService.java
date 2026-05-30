package com.delivery.deliveryplataform.orders.application.service;

import com.delivery.deliveryplataform.auth.domain.model.Driver;
import com.delivery.deliveryplataform.auth.domain.model.Restaurant;
import com.delivery.deliveryplataform.auth.domain.model.Role;
import com.delivery.deliveryplataform.auth.domain.model.User;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.DriverRepository;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.RestaurantRepository;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.UserRepository;
import com.delivery.deliveryplataform.catalog.domain.model.Dish;
import com.delivery.deliveryplataform.catalog.infrastructure.persistence.DishRepository;
import com.delivery.deliveryplataform.orders.application.dto.*;
import com.delivery.deliveryplataform.orders.domain.model.Order;
import com.delivery.deliveryplataform.orders.domain.model.OrderItem;
import com.delivery.deliveryplataform.orders.domain.model.OrderStatus;
import com.delivery.deliveryplataform.orders.infrastructure.persistence.OrderRepository;
import com.delivery.deliveryplataform.notifications.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final DriverRepository driverRepository;
    private final DishRepository dishRepository;
    private final NotificationService notificationService;

    // ========== CREAR PEDIDO ==========
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (customer.getRole() != Role.CLIENTE) {
            throw new RuntimeException("Solo los clientes pueden crear pedidos");
        }

        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurante no encontrado"));

        Order order = Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.RECIBIDA)
                .deliveryAddress(request.deliveryAddress())
                .total(0.0)
                .build();

        double total = 0.0;
        for (OrderItemRequest itemReq : request.items()) {
            Dish dish = dishRepository.findById(itemReq.dishId())
                    .orElseThrow(() -> new RuntimeException("Platillo no encontrado: " + itemReq.dishId()));

            if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
                throw new RuntimeException("El platillo " + dish.getName() + " no pertenece a este restaurante");
            }
            if (!dish.getAvailable()) {
                throw new RuntimeException("El platillo " + dish.getName() + " no está disponible");
            }

            OrderItem item = OrderItem.builder()
                    .dish(dish)
                    .quantity(itemReq.quantity())
                    .unitPrice(dish.getPrice())
                    .build();
            order.addItem(item);

            total += dish.getPrice() * itemReq.quantity();
        }
        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);

        notificationService.createNotification(
                customer.getId(),
                "Tu pedido #" + savedOrder.getId() + " ha sido recibido por el restaurante " + restaurant.getBusinessName() + ". ¡Pronto comenzarán a prepararlo!",
                "ORDER_RECEIVED"
        );

        return toResponse(savedOrder);
    }

    // ========== OBTENER UN PEDIDO ==========
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String userEmail) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        validateUserAccessToOrder(order, user);
        return toResponse(order);
    }

    // ========== LISTAR MIS PEDIDOS (según rol) ==========
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Order> orders = switch (user.getRole()) {
            case CLIENTE -> orderRepository.findByCustomerId(user.getId());
            case RESTAURANTE -> {
                Restaurant restaurant = restaurantRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Restaurante no encontrado para este usuario"));
                yield orderRepository.findByRestaurantId(restaurant.getId());
            }
            case REPARTIDOR -> {
                Driver driver = driverRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Repartidor no encontrado para este usuario"));
                yield orderRepository.findByDriverId(driver.getId());
            }
        };

        return orders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ========== CAMBIAR ESTADO ==========
    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        OrderStatus newStatus = request.status();
        OrderStatus currentStatus = order.getStatus();

        switch (newStatus) {
            case EN_PREPARACION -> {
                if (user.getRole() != Role.RESTAURANTE)
                    throw new RuntimeException("Solo el restaurante puede aceptar pedidos");
                if (currentStatus != OrderStatus.RECIBIDA)
                    throw new RuntimeException("Solo se puede aceptar un pedido en estado RECIBIDA");
                if (!order.getRestaurant().getUser().getId().equals(user.getId()))
                    throw new RuntimeException("Este pedido no es de tu restaurante");
                notificationService.createNotification(
                        order.getCustomer().getId(),
                        "¡Buenas noticias! El restaurante " + order.getRestaurant().getBusinessName() + " ya está preparando tu pedido #" + orderId + ".",
                        "ORDER_PREPARING"
                );
            }
            case EN_CAMINO -> {
                if (user.getRole() != Role.RESTAURANTE)
                    throw new RuntimeException("Solo el restaurante puede despachar pedidos");
                if (currentStatus != OrderStatus.EN_PREPARACION)
                    throw new RuntimeException("El pedido debe estar EN_PREPARACION para despacharse");
                if (request.driverId() == null)
                    throw new RuntimeException("Debes asignar un repartidor (driverId)");
                Driver driver = driverRepository.findById(request.driverId())
                        .orElseThrow(() -> new RuntimeException("Repartidor no encontrado"));
                order.setDriver(driver);
                notificationService.createNotification(
                        order.getCustomer().getId(),
                        "🛵 Tu pedido #" + orderId + " ya está en camino. Tu repartidor " + driver.getUser().getName() + " se dirige a tu dirección.",
                        "ORDER_ON_WAY"
                );
            }
            case ENTREGADA -> {
                if (user.getRole() != Role.REPARTIDOR)
                    throw new RuntimeException("Solo el repartidor puede marcar como entregado");
                if (currentStatus != OrderStatus.EN_CAMINO)
                    throw new RuntimeException("El pedido debe estar EN_CAMINO para entregarse");
                if (order.getDriver() == null || !order.getDriver().getUser().getId().equals(user.getId()))
                    throw new RuntimeException("Este pedido no está asignado a vos");
                // FIX: notificación faltante al entregar el pedido
                notificationService.createNotification(
                        order.getCustomer().getId(),
                        "✅ Tu pedido #" + orderId + " ha sido entregado. ¡Buen provecho!",
                        "ORDER_DELIVERED"
                );
            }
            default -> throw new RuntimeException("Transición de estado no permitida: " + newStatus);
        }

        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
    }

    // ========== CANCELAR PEDIDO ==========
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!order.getCustomer().getId().equals(user.getId()))
            throw new RuntimeException("Solo el cliente del pedido puede cancelarlo");
        if (order.getStatus() != OrderStatus.RECIBIDA)
            throw new RuntimeException("Solo se pueden cancelar pedidos en estado RECIBIDA");

        order.setStatus(OrderStatus.CANCELADA);

        // FIX: notificación faltante al cancelar el pedido
        notificationService.createNotification(
                order.getCustomer().getId(),
                "Tu pedido #" + orderId + " ha sido cancelado.",
                "ORDER_CANCELLED"
        );

        return toResponse(orderRepository.save(order));
    }

    // ========== HELPERS ==========
    private void validateUserAccessToOrder(Order order, User user) {
        boolean hasAccess = switch (user.getRole()) {
            case CLIENTE -> order.getCustomer().getId().equals(user.getId());
            case RESTAURANTE -> order.getRestaurant().getUser().getId().equals(user.getId());
            case REPARTIDOR -> order.getDriver() != null
                    && order.getDriver().getUser().getId().equals(user.getId());
        };
        if (!hasAccess) throw new RuntimeException("No tenés acceso a este pedido");
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getDish().getId(),
                        item.getDish().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice() * item.getQuantity()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getRestaurant().getId(),
                order.getRestaurant().getBusinessName(),
                order.getDriver() != null ? order.getDriver().getId() : null,
                order.getDriver() != null ? order.getDriver().getUser().getName() : null,
                order.getStatus(),
                order.getTotal(),
                order.getDeliveryAddress(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}