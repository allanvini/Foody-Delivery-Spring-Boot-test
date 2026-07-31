package br.com.food.demo.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.food.demo.dto.LoginRequest;
import br.com.food.demo.dto.RegisterRequest;
import br.com.food.demo.dto.RegisterResponse;
import br.com.food.demo.dto.TokenResponse;
import br.com.food.demo.dto.UserResponse;
import br.com.food.demo.entity.Role;
import br.com.food.demo.entity.User;
import br.com.food.demo.repository.RoleRepository;
import br.com.food.demo.repository.UserRepository;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "User";

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role padrão não cadastrada"));

        User user = new User();
        user.setName(resolveName(request.name(), email));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);

        User savedUser = userRepository.save(user);
        return new RegisterResponse(
                toResponse(savedUser),
                jwtService.generateToken(savedUser)
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
            );
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email ou senha inválidos",
                    exception
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Email ou senha inválidos"
                ));
        return jwtService.generateToken(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveName(String requestedName, String email) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        return email.substring(0, email.indexOf('@'));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName()
        );
    }
}
