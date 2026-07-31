package br.com.food.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @Schema(description = "ID do item", example = "1")
        @NotNull @Positive Long id,

        @Schema(example = "2")
        @NotNull @Positive Integer quantity
) {
}
