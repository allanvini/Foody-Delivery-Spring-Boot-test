package br.com.food.demo.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
