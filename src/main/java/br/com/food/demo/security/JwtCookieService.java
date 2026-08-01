package br.com.food.demo.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import br.com.food.demo.config.JwtProperties;
import br.com.food.demo.dto.UserResponse;

@Service
public class JwtCookieService {

    private static final String COOKIE_PATH = "/api";
    private static final String USER_DATA_COOKIE_NAME = "user-data";
    private static final String USER_DATA_COOKIE_PATH = "/";

    private final JwtProperties jwtProperties;
    private final JsonMapper jsonMapper;

    public JwtCookieService(JwtProperties jwtProperties, JsonMapper jsonMapper) {
        this.jwtProperties = jwtProperties;
        this.jsonMapper = jsonMapper;
    }

    public List<ResponseCookie> create(IssuedToken token, UserResponse userResponse) {
        ResponseCookie accessTokenCookie = accessTokenCookie(token.value())
                .maxAge(jwtProperties.accessTokenTtl())
                .build();
        ResponseCookie userDataCookie = userDataCookie(encodeUserData(userResponse))
                .maxAge(jwtProperties.accessTokenTtl())
                .build();

        return List.of(accessTokenCookie, userDataCookie);
    }

    public List<ResponseCookie> clear() {
        return List.of(
                accessTokenCookie("").maxAge(0).build(),
                userDataCookie("").maxAge(0).build()
        );
    }

    private ResponseCookie.ResponseCookieBuilder accessTokenCookie(String value) {
        return ResponseCookie.from(jwtProperties.cookieName(), value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite(jwtProperties.cookieSameSite())
                .path(COOKIE_PATH);
    }

    private ResponseCookie.ResponseCookieBuilder userDataCookie(String value) {
        return ResponseCookie.from(USER_DATA_COOKIE_NAME, value)
                .httpOnly(false)
                .secure(jwtProperties.cookieSecure())
                .sameSite(jwtProperties.cookieSameSite())
                .path(USER_DATA_COOKIE_PATH);
    }

    private String encodeUserData(UserResponse userResponse) {
        try {
            byte[] json = jsonMapper.writeValueAsString(userResponse)
                    .getBytes(StandardCharsets.UTF_8);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Não foi possível gerar o cookie de sessão", exception);
        }
    }
}
