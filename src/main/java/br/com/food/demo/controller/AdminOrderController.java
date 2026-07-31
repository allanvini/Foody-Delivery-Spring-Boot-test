package br.com.food.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.food.demo.dto.AdminOrderResponse;
import br.com.food.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import static br.com.food.demo.config.OpenApiConfig.BEARER_AUTH;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Administração de pedidos")
@SecurityRequirement(name = BEARER_AUTH)
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os pedidos", description = "Disponível somente para administradores.")
    public List<AdminOrderResponse> findAll() {
        return orderService.findAllForAdmin();
    }
}
