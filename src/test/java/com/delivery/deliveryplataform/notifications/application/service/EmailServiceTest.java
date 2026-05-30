package com.delivery.deliveryplataform.notifications.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - Tests unitarios")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Debe enviar email exitosamente")
    void sendEmail_success() {
        // Arrange
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendEmail("test@test.com", "Subject", "Body");

        // Assert
        verify(mailSender).send(argThat((SimpleMailMessage msg) -> {
            assertEquals("test@test.com", msg.getTo()[0]);
            assertEquals("Subject", msg.getSubject());
            assertEquals("Body", msg.getText());
            return true;
        }));
    }

    @Test
    @DisplayName("Debe capturar excepcion sin propagar cuando falla el envio")
    void sendEmail_failure_doesNotThrow() {
        // Arrange
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act (no debe lanzar excepcion)
        assertDoesNotThrow(() ->
                emailService.sendEmail("test@test.com", "Subject", "Body"));

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    private void assertEquals(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private void assertDoesNotThrow(org.junit.jupiter.api.function.Executable exec) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(exec);
    }
}