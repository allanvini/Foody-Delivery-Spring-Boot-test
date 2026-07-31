package br.com.food.demo.dto;

public record UpdateOrderStatusResponse(
        Long orderId,
        Long statusId,
        String status
) {
}
