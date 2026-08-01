package br.com.food.demo.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.food.demo.dto.LoginRequest;
import br.com.food.demo.dto.RegisterRequest;
import br.com.food.demo.dto.RegisterResponse;
import br.com.food.demo.dto.SessionResponse;
import br.com.food.demo.security.JwtCookieService;
import br.com.food.demo.service.AuthenticatedSession;
import br.com.food.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e logout")
public class AuthController {

    private final AuthService authService;
    private final JwtCookieService jwtCookieService;

    public AuthController(AuthService authService, JwtCookieService jwtCookieService) {
        this.authService = authService;
        this.jwtCookieService = jwtCookieService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar usuário", description = "Cria um usuário com role User e inicia sua sessão nos cookies.")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticatedSession session = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(cookieHeaders(jwtCookieService.create(
                        session.token(),
                        session.user()
                )))
                .body(new RegisterResponse(
                        session.user(),
                        SessionResponse.from(session.token())
                ));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Cria a sessão nos cookies access_token e user-data.")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedSession session = authService.login(request);
        return ResponseEntity.ok()
                .headers(cookieHeaders(jwtCookieService.create(
                        session.token(),
                        session.user()
                )))
                .body(SessionResponse.from(session.token()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar autenticação", description = "Remove os cookies access_token e user-data do cliente.")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .headers(cookieHeaders(jwtCookieService.clear()))
                .build();
    }

    private HttpHeaders cookieHeaders(List<ResponseCookie> cookies) {
        HttpHeaders headers = new HttpHeaders();
        cookies.forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie.toString()));
        return headers;
    }
}
