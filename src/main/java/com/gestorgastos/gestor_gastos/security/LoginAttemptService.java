package com.gestorgastos.gestor_gastos.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteccion basica contra intentos repetitivos de autenticacion (HA-005).
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_SECONDS = 300; // 5 minutos

    private static final class AttemptState {
        int failures;
        Instant blockedUntil;
    }

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        AttemptState state = attempts.get(key.trim().toLowerCase());
        if (state == null || state.blockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(state.blockedUntil)) {
            attempts.remove(key.trim().toLowerCase());
            return false;
        }
        return true;
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String normalized = key.trim().toLowerCase();
        attempts.compute(normalized, (k, state) -> {
            AttemptState current = state != null ? state : new AttemptState();
            current.failures++;
            if (current.failures >= MAX_ATTEMPTS) {
                current.blockedUntil = Instant.now().plusSeconds(BLOCK_DURATION_SECONDS);
            }
            return current;
        });
    }

    public void recordSuccess(String key) {
        if (key != null && !key.isBlank()) {
            attempts.remove(key.trim().toLowerCase());
        }
    }
}
