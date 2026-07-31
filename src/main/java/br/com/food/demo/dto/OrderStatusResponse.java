package br.com.food.demo.dto;

import br.com.food.demo.entity.OrderStatus;

public record OrderStatusResponse(
        Long id,
        String name
) {

    public static OrderStatusResponse from(OrderStatus status) {
        return new OrderStatusResponse(status.getId(), status.getName());
    }
}
