package br.com.food.demo.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @Schema(description = "Itens e quantidades do pedido")
        @NotEmpty List<@Valid CreateOrderItemRequest> items,

        @Schema(example = "Entregar na portaria")
        @Size(max = 500) String observations
) {
}
