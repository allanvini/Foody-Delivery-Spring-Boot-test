package br.com.food.demo.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import br.com.food.demo.dto.CreateOrderRequest;
import br.com.food.demo.dto.OrderResponse;
import br.com.food.demo.dto.UpdateOrderStatusRequest;
import br.com.food.demo.dto.UpdateOrderStatusResponse;
import br.com.food.demo.service.OrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> findMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.findByUserId(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.create(Long.valueOf(jwt.getSubject()), request);
    }

    @PatchMapping("/{orderId}/status")
    public UpdateOrderStatusResponse updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateStatus(orderId, request);
    }
}
