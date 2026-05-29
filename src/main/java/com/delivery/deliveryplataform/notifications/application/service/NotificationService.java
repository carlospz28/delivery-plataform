package com.delivery.deliveryplataform.notifications.application.service;

import com.delivery.deliveryplataform.auth.domain.model.User;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.UserRepository;
import com.delivery.deliveryplataform.notifications.application.dto.NotificationResponse;
import com.delivery.deliveryplataform.notifications.domain.model.Notification;
import com.delivery.deliveryplataform.notifications.infrastructure.persistence.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Este método lo usarían internamente otros servicios (ej. Orders) para enviar notificaciones
    @Transactional
    public void createNotification(Long userId, String message, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para la notificación"));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .read(false)
                .build();

        notificationRepository.save(notification);

        // Enviar correo de forma asíncrona para no bloquear la petición
        emailService.sendEmail(
                user.getEmail(),
                buildSubject(type),
                buildBody(user.getName(), message)
        );
    }

    public List<NotificationResponse> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para marcar esta notificación");
        }

        notification.setRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.getRead(),
                notification.getCreatedAt()
        );
    }

    private String buildSubject(String type) {
        return switch (type) {
            case "LOGIN_SUCCESS"   -> "🔐 Inicio de sesión exitoso - DeliveryApp";
            case "ORDER_RECEIVED"  -> "📦 Pedido recibido - DeliveryApp";
            case "ORDER_PREPARING" -> "👨‍🍳 Tu pedido está en preparación - DeliveryApp";
            case "ORDER_ON_WAY"    -> "🛵 Tu pedido está en camino - DeliveryApp";
            default                -> "🔔 Nueva notificación - DeliveryApp";
        };
    }

    private String buildBody(String userName, String message) {
        return "Hola " + userName + ",\n\n" +
               message + "\n\n" +
               "Gracias por usar DeliveryApp.\n" +
               "El equipo de DeliveryApp";
    }
}

