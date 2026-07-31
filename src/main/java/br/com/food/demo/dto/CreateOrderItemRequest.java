package br.com.food.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull @Positive Long id,
        @NotNull @Positive Integer quantity
) {
}
