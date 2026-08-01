package br.com.food.demo.dto;

public record RegisterResponse(
        UserResponse user,
        SessionResponse token
) {
}
