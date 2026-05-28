package com.delivery.deliveryplataform.catalog.infrastructure.controller;

import com.delivery.deliveryplataform.catalog.application.dto.DishRequest;
import com.delivery.deliveryplataform.catalog.application.dto.DishResponse;
import com.delivery.deliveryplataform.catalog.application.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @PostMapping
    public ResponseEntity<DishResponse> createDish(@RequestBody DishRequest request, Authentication authentication) {
        // authentication.getName() devuelve el email extraído del JWT
        DishResponse response = dishService.createDish(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<DishResponse>> getDishesByRestaurant(@PathVariable Long restaurantId) {
        List<DishResponse> response = dishService.getDishesByRestaurant(restaurantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<DishResponse>> getMyDishes(Authentication authentication) {
        List<DishResponse> response = dishService.getMyDishes(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DishResponse> updateDish(
            @PathVariable Long id,
            @RequestBody DishRequest request,
            Authentication authentication) {
        DishResponse response = dishService.updateDish(id, authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable Long id, Authentication authentication) {
        dishService.deleteDish(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<DishResponse> changeAvailability(
            @PathVariable Long id,
            @RequestParam Boolean available,
            Authentication authentication) {
        DishResponse response = dishService.changeAvailability(id, authentication.getName(), available);
        return ResponseEntity.ok(response);
    }
}
