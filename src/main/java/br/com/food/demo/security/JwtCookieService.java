package br.com.food.demo.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import br.com.food.demo.config.JwtProperties;
import br.com.food.demo.dto.TokenResponse;

@Service
public class JwtCookieService {

    private static final String COOKIE_PATH = "/api";

    private final JwtProperties jwtProperties;

    public JwtCookieService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie create(TokenResponse tokenResponse) {
        return baseCookie(tokenResponse.accessToken())
                .maxAge(jwtProperties.accessTokenTtl())
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(jwtProperties.cookieName(), value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite(jwtProperties.cookieSameSite())
                .path(COOKIE_PATH);
    }
}
