package br.com.food.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        @Schema(description = "Role do usuário autenticado", allowableValues = {"USER", "ADMIN"})
        String role
) {
}
