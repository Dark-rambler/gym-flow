package com.gymflow.auth.domain.model;

public record AccessToken(String value, long expiresInSeconds) {
}
