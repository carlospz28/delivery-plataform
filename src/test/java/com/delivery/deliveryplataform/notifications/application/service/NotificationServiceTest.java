package com.delivery.deliveryplataform.notifications.application.service;

import com.delivery.deliveryplataform.auth.domain.model.Role;
import com.delivery.deliveryplataform.auth.domain.model.User;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.UserRepository;
import com.delivery.deliveryplataform.notifications.application.dto.NotificationResponse;
import com.delivery.deliveryplataform.notifications.domain.model.Notification;
import com.delivery.deliveryplataform.notifications.infrastructure.persistence.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - Tests unitarios")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private User buildUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .name("Test User")
                .role(Role.CLIENTE)
                .build();
    }

    private Notification buildNotification(Long id, User user, String message, String type, Boolean read) {
        Notification n = Notification.builder()
                .id(id)
                .user(user)
                .message(message)
                .type(type)
                .read(read)
                .build();
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    // ==================== createNotification ====================

    @Nested
    @DisplayName("createNotification()")
    class CreateNotificationTests {

        @Test
        @DisplayName("Debe crear notificacion y enviar email exitosamente")
        void createNotification_success() {
            // Arrange
            User user = buildUser(1L, "cliente@test.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            notificationService.createNotification(1L, "Pedido recibido", "ORDER_RECEIVED");

            // Assert
            verify(notificationRepository).save(argThat(n -> {
                assertEquals("Pedido recibido", n.getMessage());
                assertEquals("ORDER_RECEIVED", n.getType());
                assertFalse(n.getRead());
                assertEquals(user, n.getUser());
                return true;
            }));
            verify(emailService).sendEmail(
                    eq("cliente@test.com"),
                    contains("Pedido recibido"),
                    contains("Test User"));
        }

        @Test
        @DisplayName("Debe enviar subject correcto para LOGIN_SUCCESS")
        void createNotification_loginSuccess_correctSubject() {
            // Arrange
            User user = buildUser(1L, "user@test.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            notificationService.createNotification(1L, "Login ok", "LOGIN_SUCCESS");

            // Assert
            verify(emailService).sendEmail(
                    eq("user@test.com"),
                    argThat(subject -> subject.contains("Inicio de sesión exitoso")),
                    anyString());
        }

        @Test
        @DisplayName("Debe enviar subject correcto para ORDER_PREPARING")
        void createNotification_orderPreparing_correctSubject() {
            // Arrange
            User user = buildUser(1L, "user@test.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            notificationService.createNotification(1L, "Preparando", "ORDER_PREPARING");

            // Assert
            verify(emailService).sendEmail(
                    eq("user@test.com"),
                    argThat(subject -> subject.contains("preparación")),
                    anyString());
        }

        @Test
        @DisplayName("Debe enviar subject correcto para ORDER_ON_WAY")
        void createNotification_orderOnWay_correctSubject() {
            // Arrange
            User user = buildUser(1L, "user@test.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            notificationService.createNotification(1L, "En camino", "ORDER_ON_WAY");

            // Assert
            verify(emailService).sendEmail(
                    eq("user@test.com"),
                    argThat(subject -> subject.contains("en camino")),
                    anyString());
        }

        @Test
        @DisplayName("Debe usar subject por defecto para tipo desconocido")
        void createNotification_unknownType_defaultSubject() {
            // Arrange
            User user = buildUser(1L, "user@test.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            notificationService.createNotification(1L, "Algo paso", "TIPO_RANDOM");

            // Assert
            verify(emailService).sendEmail(
                    eq("user@test.com"),
                    argThat(subject -> subject.contains("Nueva notificación")),
                    anyString());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si usuario no existe")
        void createNotification_userNotFound_throwsException() {
            // Arrange
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> notificationService.createNotification(99L, "msg", "TYPE"));
            assertEquals("Usuario no encontrado para la notificación", ex.getMessage());
            verify(notificationRepository, never()).save(any());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }

    // ==================== getMyNotifications ====================

    @Nested
    @DisplayName("getMyNotifications()")
    class GetMyNotificationsTests {

        @Test
        @DisplayName("Debe retornar las notificaciones del usuario")
        void getMyNotifications_success() {
            // Arrange
            User user = buildUser(1L, "cliente@test.com");
            Notification n1 = buildNotification(1L, user, "Pedido recibido", "ORDER_RECEIVED", false);
            Notification n2 = buildNotification(2L, user, "Login exitoso", "LOGIN_SUCCESS", true);

            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(user));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(n1, n2));

            // Act
            List<NotificationResponse> result = notificationService.getMyNotifications("cliente@test.com");

            // Assert
            assertEquals(2, result.size());
            assertEquals("Pedido recibido", result.get(0).message());
            assertEquals("ORDER_RECEIVED", result.get(0).type());
            assertFalse(result.get(0).read());
            assertTrue(result.get(1).read());
        }

        @Test
        @DisplayName("Debe retornar lista vacia si no hay notificaciones")
        void getMyNotifications_empty() {
            // Arrange
            User user = buildUser(1L, "nuevo@test.com");
            when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(user));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            // Act
            List<NotificationResponse> result = notificationService.getMyNotifications("nuevo@test.com");

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si usuario no existe")
        void getMyNotifications_userNotFound_throwsException() {
            // Arrange
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> notificationService.getMyNotifications("ghost@test.com"));
            assertEquals("Usuario no encontrado", ex.getMessage());
        }
    }

    // ==================== markAsRead ====================

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("Debe marcar notificacion como leida exitosamente")
        void markAsRead_success() {
            // Arrange
            User user = buildUser(1L, "cliente@test.com");
            Notification notification = buildNotification(10L, user, "Pedido listo", "ORDER_RECEIVED", false);

            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(user));
            when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            NotificationResponse result = notificationService.markAsRead(10L, "cliente@test.com");

            // Assert
            assertTrue(result.read());
            assertEquals("Pedido listo", result.message());
            verify(notificationRepository).save(argThat(n -> n.getRead().equals(true)));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si notificacion no pertenece al usuario")
        void markAsRead_notOwner_throwsException() {
            // Arrange
            User owner = buildUser(1L, "owner@test.com");
            User other = buildUser(2L, "other@test.com");
            Notification notification = buildNotification(10L, owner, "msg", "TYPE", false);

            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));
            when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> notificationService.markAsRead(10L, "other@test.com"));
            assertEquals("No tienes permiso para marcar esta notificación", ex.getMessage());
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si notificacion no existe")
        void markAsRead_notificationNotFound_throwsException() {
            // Arrange
            User user = buildUser(1L, "user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> notificationService.markAsRead(999L, "user@test.com"));
            assertEquals("Notificación no encontrada", ex.getMessage());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si usuario no existe")
        void markAsRead_userNotFound_throwsException() {
            // Arrange
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> notificationService.markAsRead(1L, "ghost@test.com"));
            assertEquals("Usuario no encontrado", ex.getMessage());
        }
    }
}