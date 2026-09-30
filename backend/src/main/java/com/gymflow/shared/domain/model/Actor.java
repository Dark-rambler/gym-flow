package com.gymflow.shared.domain.model;

// Usuario autenticado que ejecuta un caso de uso (sale del JWT, nunca del request).
public record Actor(Long userId, Long gymId, Role role) {

    public boolean is(Role r) {
        return role == r;
    }
}
