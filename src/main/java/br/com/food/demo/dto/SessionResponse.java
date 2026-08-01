package br.com.food.demo.dto;

import br.com.food.demo.security.IssuedToken;
import io.swagger.v3.oas.annotations.media.Schema;

public record SessionResponse(
        String tokenType,
        long expiresIn,
        @Schema(description = "Role do usuário autenticado", allowableValues = {"USER", "ADMIN"})
        String role
) {
    public static SessionResponse from(IssuedToken token) {
        return new SessionResponse(token.tokenType(), token.expiresIn(), token.role());
    }
}
