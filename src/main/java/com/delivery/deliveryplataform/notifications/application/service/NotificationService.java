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
}
