package br.com.food.demo.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.food.demo.dto.LoginRequest;
import br.com.food.demo.dto.RegisterRequest;
import br.com.food.demo.dto.RegisterResponse;
import br.com.food.demo.dto.TokenResponse;
import br.com.food.demo.security.JwtCookieService;
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
    @Operation(summary = "Cadastrar usuário", description = "Cria um usuário com role User e já retorna seu JWT.")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse registerResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, jwtCookieService.create(registerResponse.token()).toString())
                .body(registerResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Retorna o JWT no corpo e no cookie access_token.")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookieService.create(tokenResponse).toString())
                .body(tokenResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar autenticação", description = "Remove o cookie access_token do cliente.")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtCookieService.clear().toString())
                .build();
    }
}
