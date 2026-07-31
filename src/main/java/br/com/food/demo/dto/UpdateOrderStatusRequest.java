package br.com.food.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateOrderStatusRequest(
        @Schema(description = "ID obtido em GET /api/order-statuses", example = "2")
        @NotNull @Positive Long statusId
) {
}
