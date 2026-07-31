package br.com.food.demo.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ItemRequest(
        @Schema(example = "Hambúrguer Artesanal")
        @NotBlank @Size(max = 150) String name,

        @Schema(example = "29.90")
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,

        @Schema(example = "20")
        @NotNull @PositiveOrZero Integer stock
) {
}
