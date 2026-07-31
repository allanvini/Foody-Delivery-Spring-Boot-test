package br.com.food.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateOrderStatusRequest(
        @NotNull @Positive Long statusId
) {
}
