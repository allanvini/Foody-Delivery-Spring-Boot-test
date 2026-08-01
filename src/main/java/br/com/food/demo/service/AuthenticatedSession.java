package br.com.food.demo.service;

import br.com.food.demo.dto.UserResponse;
import br.com.food.demo.security.IssuedToken;

public record AuthenticatedSession(
        UserResponse user,
        IssuedToken token
) {
}
