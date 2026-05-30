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
import com.delivery.deliveryplataform.notifications.application.service.NotificationService;
import com.delivery.deliveryplataform.orders.application.dto.*;
import com.delivery.deliveryplataform.orders.domain.model.Order;
import com.delivery.deliveryplataform.orders.domain.model.OrderItem;
import com.delivery.deliveryplataform.orders.domain.model.OrderStatus;
import com.delivery.deliveryplataform.orders.infrastructure.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService - Tests unitarios")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private DishRepository dishRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private User cliente;
    private User restauranteUser;
    private User repartidorUser;
    private Restaurant restaurant;
    private Driver driver;
    private Dish dish;

    @BeforeEach
    void setUp() {
        cliente = User.builder()
                .id(1L).email("cliente@test.com").name("Juan")
                .role(Role.CLIENTE).active(true).build();

        restauranteUser = User.builder()
                .id(2L).email("resto@test.com").name("Don Pepe")
                .role(Role.RESTAURANTE).active(true).build();

        repartidorUser = User.builder()
                .id(3L).email("repar@test.com").name("Pedro")
                .role(Role.REPARTIDOR).active(true).build();

        restaurant = Restaurant.builder()
                .id(1L).user(restauranteUser)
                .businessName("Pizzería Don Pepe")
                .address("Av Siempre Viva 742").open(true).build();

        driver = Driver.builder()
                .id(1L).user(repartidorUser)
                .vehicleType("MOTO").licensePlate("ABC123")
                .available(true).build();

        dish = Dish.builder()
                .id(1L).restaurant(restaurant)
                .name("Pizza Hawaiana").price(25000.0)
                .available(true).build();
    }

    private Order buildOrder(Long id, OrderStatus status) {
        Order order = Order.builder()
                .id(id)
                .customer(cliente)
                .restaurant(restaurant)
                .status(status)
                .total(50000.0)
                .deliveryAddress("Calle 123, Mi Casa")
                .items(new ArrayList<>())
                .build();
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        OrderItem item = OrderItem.builder()
                .id(1L).order(order).dish(dish)
                .quantity(2).unitPrice(25000.0).build();
        order.getItems().add(item);

        if (status == OrderStatus.EN_CAMINO || status == OrderStatus.ENTREGADA) {
            order.setDriver(driver);
        }
        return order;
    }

    // ==================== createOrder ====================

    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTests {

        @Test
        @DisplayName("Debe crear pedido exitosamente como CLIENTE")
        void createOrder_success() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest(
                    1L, "Calle 123, Mi Casa",
                    List.of(new OrderItemRequest(1L, 2))
            );

            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
            when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(1L);
                o.setCreatedAt(LocalDateTime.now());
                o.setUpdatedAt(LocalDateTime.now());
                return o;
            });

            // Act
            OrderResponse response = orderService.createOrder(request, "cliente@test.com");

            // Assert
            assertNotNull(response);
            assertEquals(OrderStatus.RECIBIDA, response.status());
            assertEquals(50000.0, response.total());
            assertEquals("Calle 123, Mi Casa", response.deliveryAddress());

            verify(orderRepository).save(any(Order.class));
            verify(notificationService).createNotification(
                    eq(1L), anyString(), eq("ORDER_RECEIVED"));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si usuario no es CLIENTE")
        void createOrder_notCliente_throwsException() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest(
                    1L, "Calle 123", List.of(new OrderItemRequest(1L, 1))
            );
            when(userRepository.findByEmail("resto@test.com"))
                    .thenReturn(Optional.of(restauranteUser));

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(request, "cliente@test.com"));
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si restaurante no existe")
        void createOrder_restaurantNotFound_throwsException() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest(
                    99L, "Calle 123", List.of(new OrderItemRequest(1L, 1))
            );
            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
            when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(request, "cliente@test.com"));
            assertTrue(ex.getMessage().toLowerCase().contains("restaurante"));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si plato no pertenece al restaurante")
        void createOrder_dishNotFromRestaurant_throwsException() {
            // Arrange
            Restaurant otherResto = Restaurant.builder().id(2L).build();
            Dish otherDish = Dish.builder()
                    .id(5L).restaurant(otherResto)
                    .name("Otra Pizza").price(30000.0)
                    .available(true).build();

            CreateOrderRequest request = new CreateOrderRequest(
                    1L, "Calle 123", List.of(new OrderItemRequest(5L, 1))
            );
            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
            when(dishRepository.findById(5L)).thenReturn(Optional.of(otherDish));

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(request, "cliente@test.com"));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si plato no esta disponible")
        void createOrder_dishNotAvailable_throwsException() {
            // Arrange
            Dish unavailableDish = Dish.builder()
                    .id(1L).restaurant(restaurant)
                    .name("Pizza Agotada").price(25000.0)
                    .available(false).build();

            CreateOrderRequest request = new CreateOrderRequest(
                    1L, "Calle 123", List.of(new OrderItemRequest(1L, 1))
            );
            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
            when(dishRepository.findById(1L)).thenReturn(Optional.of(unavailableDish));

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(request, "cliente@test.com"));
        }
    }

    // ==================== getMyOrders ====================

    @Nested
    @DisplayName("getMyOrders()")
    class GetMyOrdersTests {

        @Test
        @DisplayName("Debe retornar pedidos del CLIENTE")
        void getMyOrders_asCliente() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.RECIBIDA);
            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
            when(orderRepository.findByCustomerId(1L)).thenReturn(List.of(order));

            // Act
            List<OrderResponse> result = orderService.getMyOrders("cliente@test.com");

            // Assert
            assertEquals(1, result.size());
            verify(orderRepository).findByCustomerId(1L);
        }

        @Test
        @DisplayName("Debe retornar pedidos del RESTAURANTE")
        void getMyOrders_asRestaurante() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.RECIBIDA);
            when(userRepository.findByEmail("resto@test.com")).thenReturn(Optional.of(restauranteUser));
            when(restaurantRepository.findByUserId(2L)).thenReturn(Optional.of(restaurant));
            when(orderRepository.findByRestaurantId(1L)).thenReturn(List.of(order));

            // Act
            List<OrderResponse> result = orderService.getMyOrders("resto@test.com");

            // Assert
            assertEquals(1, result.size());
            verify(orderRepository).findByRestaurantId(1L);
        }

        @Test
        @DisplayName("Debe retornar pedidos del REPARTIDOR")
        void getMyOrders_asRepartidor() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.EN_CAMINO);
            when(userRepository.findByEmail("repar@test.com")).thenReturn(Optional.of(repartidorUser));
            when(driverRepository.findByUserId(3L)).thenReturn(Optional.of(driver));
            when(orderRepository.findByDriverId(1L)).thenReturn(List.of(order));

            // Act
            List<OrderResponse> result = orderService.getMyOrders("repar@test.com");

            // Assert
            assertEquals(1, result.size());
            verify(orderRepository).findByDriverId(1L);
        }
    }

    // ==================== updateStatus ====================

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("RESTAURANTE acepta pedido: RECIBIDA -> EN_PREPARACION")
        void updateStatus_recibirToEnPreparacion_success() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.RECIBIDA);
            UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(
                    OrderStatus.EN_PREPARACION, null);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userRepository.findByEmail("resto@test.com")).thenReturn(Optional.of(restauranteUser));
            when(restaurantRepository.findByUserId(2L)).thenReturn(Optional.of(restaurant));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            OrderResponse response = orderService.updateStatus(1L, request, "resto@test.com");

            // Assert
            assertNotNull(response);
            verify(orderRepository).save(argThat(o ->
                    o.getStatus() == OrderStatus.EN_PREPARACION));
            verify(notificationService).createNotification(
                    anyLong(), anyString(), eq("ORDER_PREPARING"));
        }

        @Test
        @DisplayName("RESTAURANTE despacha con repartidor: EN_PREPARACION -> EN_CAMINO")
        void updateStatus_enPreparacionToEnCamino_success() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.EN_PREPARACION);
            UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(
                    OrderStatus.EN_CAMINO, 1L);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userRepository.findByEmail("resto@test.com")).thenReturn(Optional.of(restauranteUser));
            when(restaurantRepository.findByUserId(2L)).thenReturn(Optional.of(restaurant));
            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            OrderResponse response = orderService.updateStatus(1L, request, "resto@test.com");

            // Assert
            assertNotNull(response);
            verify(orderRepository).save(argThat(o ->
                    o.getStatus() == OrderStatus.EN_CAMINO && o.getDriver() != null));
            verify(notificationService).createNotification(
                    anyLong(), anyString(), eq("ORDER_ON_WAY"));
        }

        @Test
        @DisplayName("REPARTIDOR confirma entrega: EN_CAMINO -> ENTREGADA")
        void updateStatus_enCaminoToEntregada_success() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.EN_CAMINO);
            UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(
                    OrderStatus.ENTREGADA, null);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userRepository.findByEmail("repar@test.com")).thenReturn(Optional.of(repartidorUser));
            when(driverRepository.findByUserId(3L)).thenReturn(Optional.of(driver));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            OrderResponse response = orderService.updateStatus(1L, request, "repar@test.com");

            // Assert
            assertNotNull(response);
            verify(orderRepository).save(argThat(o ->
                    o.getStatus() == OrderStatus.ENTREGADA));
            verify(notificationService).createNotification(
                    anyLong(), anyString(), eq("ORDER_DELIVERED"));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si pedido no existe")
        void updateStatus_orderNotFound_throwsException() {
            // Arrange
            UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(
                    OrderStatus.EN_PREPARACION, null);
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> orderService.updateStatus(99L, request, "resto@test.com"));
        }
    }

    // ==================== cancelOrder ====================

    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrderTests {

        @Test
        @DisplayName("CLIENTE puede cancelar pedido RECIBIDA")
        void cancelOrder_success() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.RECIBIDA);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            OrderResponse response = orderService.cancelOrder(1L, "cliente@test.com");

            // Assert
            assertNotNull(response);
            verify(orderRepository).save(argThat(o ->
                    o.getStatus() == OrderStatus.CANCELADA));
            verify(notificationService).createNotification(
                    anyLong(), anyString(), eq("ORDER_CANCELLED"));
        }

        @Test
        @DisplayName("No se puede cancelar pedido que no esta en RECIBIDA")
        void cancelOrder_wrongStatus_throwsException() {
            // Arrange
            Order order = buildOrder(1L, OrderStatus.EN_PREPARACION);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> orderService.cancelOrder(1L, "cliente@test.com"));
            verify(orderRepository, never()).save(any());
        }
    }
}