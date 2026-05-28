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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Validar que el email no exista
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // 2. Crear y guardar el usuario base (con la contraseña encriptada)
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .role(request.role())
                .active(true)
                .build();
        User savedUser = userRepository.save(user);

        // 3. Crear info adicional según el rol
        if (request.role() == Role.RESTAURANTE) {
            if (request.businessName() == null || request.address() == null) {
                throw new RuntimeException("Un restaurante requiere businessName y address");
            }
            Restaurant restaurant = Restaurant.builder()
                    .user(savedUser)
                    .businessName(request.businessName())
                    .address(request.address())
                    .open(true)
                    .build();
            restaurantRepository.save(restaurant);
        } else if (request.role() == Role.REPARTIDOR) {
            if (request.vehicleType() == null || request.licensePlate() == null) {
                throw new RuntimeException("Un repartidor requiere vehicleType y licensePlate");
            }
            Driver driver = Driver.builder()
                    .user(savedUser)
                    .vehicleType(request.vehicleType())
                    .licensePlate(request.licensePlate())
                    .available(true)
                    .build();
            driverRepository.save(driver);
        }

        // 4. Generar token y devolver respuesta
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(),
                savedUser.getName(), savedUser.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Autenticar (lanza BadCredentialsException si email/password están mal)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        // 2. Cargar el usuario
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Generar token y devolver
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}