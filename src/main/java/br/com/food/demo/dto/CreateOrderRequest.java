package br.com.food.demo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotEmpty List<@Valid CreateOrderItemRequest> items,
        @Size(max = 500) String observations
) {
}
