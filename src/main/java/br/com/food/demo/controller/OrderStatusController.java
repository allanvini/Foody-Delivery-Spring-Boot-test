package br.com.food.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.food.demo.dto.OrderStatusResponse;
import br.com.food.demo.service.OrderStatusService;

@RestController
@RequestMapping("/api/order-statuses")
public class OrderStatusController {

    private final OrderStatusService orderStatusService;

    public OrderStatusController(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @GetMapping
    public List<OrderStatusResponse> findAll() {
        return orderStatusService.findAll();
    }
}
