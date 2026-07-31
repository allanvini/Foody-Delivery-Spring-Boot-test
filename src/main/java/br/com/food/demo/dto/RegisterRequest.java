package br.com.food.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Cliente Teste")
        @NotBlank @Size(max = 150) String name,

        @Schema(example = "Rua das Flores, 123")
        @NotBlank @Size(max = 500) String address,

        @Schema(example = "cliente@example.com")
        @NotBlank @Email @Size(max = 255) String email,

        @Schema(example = "senha-segura")
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
