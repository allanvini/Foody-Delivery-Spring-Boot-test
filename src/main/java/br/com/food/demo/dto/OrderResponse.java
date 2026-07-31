package br.com.food.demo.dto;

import java.math.BigDecimal;
import java.util.List;

import br.com.food.demo.entity.Order;

public record OrderResponse(
        Long id,
        String status,
        BigDecimal total,
        String observations,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().getName(),
                order.getTotal(),
                order.getObservations(),
                items
        );
    }
}
