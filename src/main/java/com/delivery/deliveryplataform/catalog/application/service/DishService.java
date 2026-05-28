package com.delivery.deliveryplataform.catalog.application.service;

import com.delivery.deliveryplataform.auth.domain.model.Restaurant;
import com.delivery.deliveryplataform.auth.domain.model.Role;
import com.delivery.deliveryplataform.auth.domain.model.User;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.RestaurantRepository;
import com.delivery.deliveryplataform.auth.infrastructure.persistence.UserRepository;
import com.delivery.deliveryplataform.catalog.application.dto.DishRequest;
import com.delivery.deliveryplataform.catalog.application.dto.DishResponse;
import com.delivery.deliveryplataform.catalog.domain.model.Dish;
import com.delivery.deliveryplataform.catalog.infrastructure.persistence.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public DishResponse createDish(String email, DishRequest request) {
        Restaurant restaurant = getRestaurantByEmail(email);

        Dish dish = Dish.builder()
                .restaurant(restaurant)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .available(request.available() != null ? request.available() : true)
                .build();

        Dish savedDish = dishRepository.save(dish);
        return mapToResponse(savedDish);
    }

    public List<DishResponse> getDishesByRestaurant(Long restaurantId) {
        return dishRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DishResponse> getMyDishes(String email) {
        Restaurant restaurant = getRestaurantByEmail(email);
        return getDishesByRestaurant(restaurant.getId());
    }

    @Transactional
    public DishResponse updateDish(Long id, String email, DishRequest request) {
        Restaurant restaurant = getRestaurantByEmail(email);
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platillo no encontrado"));

        if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
            throw new RuntimeException("No tienes permiso para actualizar este platillo");
        }

        dish.setName(request.name());
        dish.setDescription(request.description());
        dish.setPrice(request.price());
        if (request.available() != null) {
            dish.setAvailable(request.available());
        }

        Dish updatedDish = dishRepository.save(dish);
        return mapToResponse(updatedDish);
    }

    @Transactional
    public void deleteDish(Long id, String email) {
        Restaurant restaurant = getRestaurantByEmail(email);
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platillo no encontrado"));

        if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar este platillo");
        }

        dishRepository.delete(dish);
    }

    @Transactional
    public DishResponse changeAvailability(Long id, String email, Boolean available) {
        Restaurant restaurant = getRestaurantByEmail(email);
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platillo no encontrado"));

        if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
            throw new RuntimeException("No tienes permiso para modificar este platillo");
        }

        dish.setAvailable(available);
        return mapToResponse(dishRepository.save(dish));
    }

    private Restaurant getRestaurantByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (user.getRole() != Role.RESTAURANTE) {
            throw new RuntimeException("Solo los restaurantes pueden realizar esta acción");
        }
        return restaurantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Restaurante no encontrado para este usuario"));
    }

    private DishResponse mapToResponse(Dish dish) {
        return new DishResponse(
                dish.getId(),
                dish.getRestaurant().getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getPrice(),
                dish.getAvailable(),
                dish.getCreatedAt()
        );
    }
}
