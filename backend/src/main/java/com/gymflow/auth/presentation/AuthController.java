package com.gymflow.auth.presentation;

import com.gymflow.auth.application.dto.AuthResponse;
import com.gymflow.auth.application.dto.LoginRequest;
import com.gymflow.auth.application.dto.RefreshRequest;
import com.gymflow.auth.application.dto.RegisterGymRequest;
import com.gymflow.auth.application.usecase.LoginUseCase;
import com.gymflow.auth.application.usecase.LogoutUseCase;
import com.gymflow.auth.application.usecase.RefreshSessionUseCase;
import com.gymflow.auth.application.usecase.RegisterGymUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Rutas públicas (PUBLIC_PATHS en SecurityConfig).
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterGymUseCase registerGym;
    private final LoginUseCase login;
    private final RefreshSessionUseCase refresh;
    private final LogoutUseCase logout;

    @Operation(operationId = "registerGym", summary = "POST /api/auth/register-gym — registra un gimnasio y su OWNER")
    @PostMapping("/register-gym")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerGym(@Valid @RequestBody RegisterGymRequest req) {
        return registerGym.execute(req);
    }

    @Operation(operationId = "login", summary = "POST /api/auth/login — inicia sesión con email y contraseña")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return login.execute(req);
    }

    @Operation(operationId = "refresh", summary = "POST /api/auth/refresh — rota el refresh token y emite un access token nuevo")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return refresh.execute(req.refreshToken());
    }

    @Operation(operationId = "logout", summary = "POST /api/auth/logout — revoca el refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        logout.execute(req.refreshToken());
    }
}
