package br.com.food.demo.security;

public record IssuedToken(
        String value,
        String tokenType,
        long expiresIn,
        String role
) {
}
