package com.delivery.deliveryplataform.orders.domain.model;

import com.delivery.deliveryplataform.catalog.domain.model.Dish;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(nullable = false)
    private Integer quantity;

    // Guardamos el precio al momento de la orden (porque el precio del plato puede cambiar después)
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;
}