package br.com.food.demo.dto;

import java.math.BigDecimal;
import java.util.List;

import br.com.food.demo.entity.Order;
import br.com.food.demo.entity.User;

public record AdminOrderResponse(
        Long id,
        UserResponse user,
        String status,
        BigDecimal total,
        String observations,
        List<OrderItemResponse> items
) {

    public static AdminOrderResponse from(Order order, List<OrderItemResponse> items) {
        User user = order.getUser();
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAddress(),
                user.getRole().getName()
        );

        return new AdminOrderResponse(
                order.getId(),
                userResponse,
                order.getStatus().getName(),
                order.getTotal(),
                order.getObservations(),
                items
        );
    }
}
