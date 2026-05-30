package com.delivery.deliveryplataform.auth.application.service;

import com.delivery.deliveryplataform.auth.application.dto.AuthResponse;
import com.delivery.deliveryplataform.auth.application.dto.LoginRequest;
import com.delivery.deliveryplataform.auth.application.dto.RegisterRequest;
import com.delivery.deliveryplataform.auth.domain.model.Driver;
import com.delivery.deliveryplataform.auth.domain.model.Restaurant;
import com.delivery.deliveryplataform.auth.domain.model.Role;
import com.delivery.deliveryplataform.auth.domain.model.User;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.DriverRepository;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.RestaurantRepository;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.UserRepository;
import com.delivery.deliveryplataform.auth.infrastructure.security.JwtUtil;
import com.delivery.deliveryplataform.notifications.application.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests unitarios")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Debe registrar un CLIENTE exitosamente")
        void register_cliente_success() {
            // Arrange
            // RegisterRequest: email, password, name, phone, role, businessName, address, vehicleType, licensePlate
            RegisterRequest request = new RegisterRequest(
                    "cliente@test.com", "password123", "Juan", "3001234567", Role.CLIENTE,
                    null, null, null, null
            );

            when(userRepository.existsByEmail("cliente@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtUtil.generateToken("cliente@test.com", "CLIENTE")).thenReturn("jwt.token.here");

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertNotNull(response);
            assertEquals("cliente@test.com", response.email());
            assertEquals("Juan", response.name());
            assertEquals(Role.CLIENTE, response.role());
            assertEquals("jwt.token.here", response.token());

            verify(userRepository).save(any(User.class));
            verify(restaurantRepository, never()).save(any());
            verify(driverRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe registrar un RESTAURANTE con datos adicionales")
        void register_restaurante_success() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "resto@test.com", "password123", "Don Pepe", "3009998888", Role.RESTAURANTE,
                    "Pizzería Don Pepe", "Av Siempre Viva 742",
                    null, null
            );

            when(userRepository.existsByEmail("resto@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });
            when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateToken("resto@test.com", "RESTAURANTE")).thenReturn("jwt.resto.token");

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertNotNull(response);
            assertEquals(Role.RESTAURANTE, response.role());
            verify(restaurantRepository).save(any(Restaurant.class));
            verify(driverRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe registrar un REPARTIDOR con datos adicionales")
        void register_repartidor_success() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "repar@test.com", "password123", "Pedro", "3007776666", Role.REPARTIDOR,
                    null, null,
                    "MOTO", "ABC123"
            );

            when(userRepository.existsByEmail("repar@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(3L);
                return u;
            });
            when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateToken("repar@test.com", "REPARTIDOR")).thenReturn("jwt.repar.token");

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertNotNull(response);
            assertEquals(Role.REPARTIDOR, response.role());
            verify(driverRepository).save(any(Driver.class));
            verify(restaurantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si el email ya existe")
        void register_duplicateEmail_throwsException() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "existe@test.com", "password123", "Test", "3001111111", Role.CLIENTE,
                    null, null, null, null
            );
            when(userRepository.existsByEmail("existe@test.com")).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertEquals("El email ya está registrado", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si RESTAURANTE no tiene businessName")
        void register_restaurante_missingBusinessName_throwsException() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "resto2@test.com", "password123", "Test", "3002222222", Role.RESTAURANTE,
                    null, null, null, null
            );
            when(userRepository.existsByEmail("resto2@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(4L);
                return u;
            });

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertEquals("Un restaurante requiere businessName y address", ex.getMessage());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si REPARTIDOR no tiene vehicleType")
        void register_repartidor_missingVehicleType_throwsException() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "repar2@test.com", "password123", "Test", "3003333333", Role.REPARTIDOR,
                    null, null, null, null
            );
            when(userRepository.existsByEmail("repar2@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(5L);
                return u;
            });

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertEquals("Un repartidor requiere vehicleType y licensePlate", ex.getMessage());
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Debe loguear exitosamente y crear notificacion")
        void login_success() {
            // Arrange
            LoginRequest request = new LoginRequest("cliente@test.com", "password123");
            User user = User.builder()
                    .id(1L)
                    .email("cliente@test.com")
                    .password("$2a$encoded")
                    .name("Juan")
                    .role(Role.CLIENTE)
                    .build();

            when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken("cliente@test.com", "CLIENTE")).thenReturn("jwt.login.token");

            // Act
            AuthResponse response = authService.login(request);

            // Assert
            assertNotNull(response);
            assertEquals("jwt.login.token", response.token());
            assertEquals("cliente@test.com", response.email());
            assertEquals(Role.CLIENTE, response.role());

            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class));
            verify(notificationService).createNotification(
                    eq(1L), anyString(), eq("LOGIN_SUCCESS"));
        }

        @Test
        @DisplayName("Debe lanzar excepcion con credenciales invalidas")
        void login_badCredentials_throwsException() {
            // Arrange
            LoginRequest request = new LoginRequest("cliente@test.com", "wrongpass");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThrows(BadCredentialsException.class,
                    () -> authService.login(request));
            verify(notificationService, never()).createNotification(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Debe lanzar excepcion si usuario no existe despues de autenticar")
        void login_userNotFound_throwsException() {
            // Arrange
            LoginRequest request = new LoginRequest("noexiste@test.com", "password123");
            when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(request));
            assertEquals("Usuario no encontrado", ex.getMessage());
        }
    }
}