package br.com.food.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        Long id,
        String name,
        String email,
        String address,
        @Schema(description = "Role do usuário", allowableValues = {"USER", "ADMIN"})
        String role
) {
}
