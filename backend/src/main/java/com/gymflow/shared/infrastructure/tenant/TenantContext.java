package com.gymflow.shared.infrastructure.tenant;

import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Override explícito del tenant para código que corre sin JWT (registro, login, refresh, jobs).
 * Hibernate fija el tenant al abrir la sesión, así que la transacción debe abrirse DENTRO de callAs/callAsSystem.
 */
public final class TenantContext {

    /** Tenant "root": Hibernate desactiva el filtro @TenantId. Solo para flujos de sistema explícitos. */
    public static final long SYSTEM = -1L;
    /** Sin tenant: no coincide con ningún gym, así que las consultas no devuelven nada. */
    public static final long NONE = 0L;

    private static final ThreadLocal<Long> OVERRIDE = new ThreadLocal<>();

    private TenantContext() {
    }

    public static <T> T callAs(long gymId, Supplier<T> fn) {
        // Dentro de una transacción ya abierta la sesión de Hibernate tiene su tenant fijado: el override no haría nada.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("TenantContext.callAs debe llamarse fuera de una transacción");
        }
        Long previous = OVERRIDE.get();
        OVERRIDE.set(gymId);
        try {
            return fn.get();
        } finally {
            restore(previous);
        }
    }

    public static <T> T callAsSystem(Supplier<T> fn) {
        return callAs(SYSTEM, fn);
    }

    static Long override() {
        return OVERRIDE.get();
    }

    private static void restore(Long previous) {
        if (previous == null) {
            OVERRIDE.remove();
        } else {
            OVERRIDE.set(previous);
        }
    }
}
