package br.com.food.demo.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.food.demo.dto.OrderItemResponse;
import br.com.food.demo.dto.OrderResponse;
import br.com.food.demo.entity.Order;
import br.com.food.demo.repository.OrderItemRepository;
import br.com.food.demo.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUserId(Long userId) {
        List<Order> orders = orderRepository.findAllByUserIdOrderByIdDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItemResponse>> itemsByOrderId = orderItemRepository
                .findAllByOrder_IdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        orderItem -> orderItem.getOrder().getId(),
                        Collectors.mapping(OrderItemResponse::from, Collectors.toList())
                ));

        return orders.stream()
                .map(order -> OrderResponse.from(
                        order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of())
                ))
                .toList();
    }
}
