package com.delivery.deliveryplataform.auth.infrastructure.persistence;

import com.delivery.deliveryplataform.auth.domain.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByUserId(Long userId);
}