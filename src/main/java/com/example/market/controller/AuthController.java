package com.example.market.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.market.dto.AuthLoginRequest;
import com.example.market.dto.AuthRefreshRequest;
import com.example.market.dto.AuthRegisterRequest;
import com.example.market.dto.AuthTokenResponse;
import com.example.market.model.User;
import com.example.market.repository.UserRepository;
import com.example.market.service.TokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRegisterRequest request) {
        if (request.password().length() < 8 || !request.password().matches(".*[!@#$%^&*].*")) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Пароль должен содержать минимум 8 символов и один спецсимвол")
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Пользователь с таким email уже существует")
            );
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Пользователь успешно зарегистрирован"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthLoginRequest request,
                                   HttpServletRequest httpRequest) {
        try {
            TokenService.TokenPair tokens = tokenService.authenticate(
                    request.email(),
                    request.password(),
                    httpRequest
            );

            return ResponseEntity.ok(AuthTokenResponse.from(tokens));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Неверный email или пароль"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody AuthRefreshRequest request,
                                     HttpServletRequest httpRequest) {
        try {
            TokenService.TokenPair tokens = tokenService.refreshTokens(
                    request.refreshToken(),
                    httpRequest
            );

            return ResponseEntity.ok(AuthTokenResponse.from(tokens));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Недействительный refresh токен"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody AuthRefreshRequest request) {
        tokenService.logout(request.refreshToken());
        return ResponseEntity.ok(Map.of("message", "Выход из системы выполнен"));
    }

    @PostMapping("/logout/all")
    public ResponseEntity<?> logoutAll(Authentication authentication) {
        tokenService.logoutAllSessions(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Все активные refresh-сессии отозваны"));
    }
}