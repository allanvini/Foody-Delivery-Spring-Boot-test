package br.com.food.demo.dto;

import java.math.BigDecimal;

import br.com.food.demo.entity.OrderItem;

public record OrderItemResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer quantity
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getItem().getId(),
                orderItem.getItem().getName(),
                orderItem.getItem().getPrice(),
                orderItem.getQuantity()
        );
    }
}
