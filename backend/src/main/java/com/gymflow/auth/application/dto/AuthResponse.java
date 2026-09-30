package com.gymflow.auth.application.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn, MeResponse user) {
}
