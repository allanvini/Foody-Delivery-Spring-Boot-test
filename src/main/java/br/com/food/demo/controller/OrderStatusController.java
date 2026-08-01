package br.com.food.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.food.demo.dto.OrderStatusResponse;
import br.com.food.demo.service.OrderStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import static br.com.food.demo.config.OpenApiConfig.COOKIE_AUTH;

@RestController
@RequestMapping("/api/order-statuses")
@Tag(name = "Status de pedidos")
@SecurityRequirement(name = COOKIE_AUTH)
public class OrderStatusController {

    private final OrderStatusService orderStatusService;

    public OrderStatusController(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @GetMapping
    @Operation(summary = "Listar status disponíveis")
    public List<OrderStatusResponse> findAll() {
        return orderStatusService.findAll();
    }
}
