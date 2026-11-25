package com.gestorgastos.gestor_gastos.service.ia;

import com.gestorgastos.gestor_gastos.entity.ConsumoRecurso;
import com.gestorgastos.gestor_gastos.repository.ConsumoRecursoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

/**
 * Administrador centralizado de recursos externos (B3 / ResourceManager V1).
 *
 * Controla habilitacion de proveedores, cuotas por minuto y dia (por usuario y globales),
 * reserva de capacidad, concurrencia maxima y circuit breaker ante fallos HTTP.
 * Limite por minuto en memoria; consumo diario persistido en ConsumoRecurso.
 */
@Service
public class ResourceManager {

    private final ConsumoRecursoRepository consumoRecursoRepository;

    @Value("${nexus.ai.groq.enabled:false}")
    private boolean groqEnabled;

    @Value("${nexus.ai.groq.api-key:}")
    private String groqApiKey;

    @Value("${nexus.ai.resource.global-rpm:20}")
    private int globalRpmLimit;

    @Value("${nexus.ai.resource.user-rpm:5}")
    private int userRpmLimit;

    @Value("${nexus.ai.resource.global-rpd:500}")
    private int globalRpdLimit;

    @Value("${nexus.ai.resource.user-rpd:50}")
    private int userRpdLimit;

    @Value("${nexus.ai.resource.daily-token-budget:150000}")
    private long dailyTokenBudget;

    @Value("${nexus.ai.resource.tpm-limit:6000}")
    private long tpmLimit;

    @Value("${nexus.ai.resource.reserve-percent:10}")
    private int reservePercent;

    @Value("${nexus.ai.resource.max-concurrent:2}")
    private int maxConcurrent;

    @Value("${nexus.ai.resource.circuit-failure-threshold:3}")
    private int circuitFailureThreshold;

    @Value("${nexus.ai.resource.circuit-open-seconds:60}")
    private long circuitOpenSeconds;

    private record TokenWindowEntry(Instant timestamp, long tokens) {}

    private final Deque<Instant> globalMinuteWindow = new ConcurrentLinkedDeque<>();
    private final Deque<TokenWindowEntry> globalTokenMinuteWindow = new ConcurrentLinkedDeque<>();
    private final Map<Long, Deque<Instant>> userMinuteWindows = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();
    private final Map<String, Instant> circuitOpenUntil = new ConcurrentHashMap<>();
    private volatile Semaphore concurrencySemaphore;

    public ResourceManager(ConsumoRecursoRepository consumoRecursoRepository) {
        this.consumoRecursoRepository = consumoRecursoRepository;
    }

    private Semaphore getSemaphore() {
        if (concurrencySemaphore == null) {
            synchronized (this) {
                if (concurrencySemaphore == null) {
                    concurrencySemaphore = new Semaphore(Math.max(1, maxConcurrent));
                }
            }
        }
        return concurrencySemaphore;
    }

    public boolean isProviderAuthorizedAndEnabled(String provider) {
        if ("GROQ".equals(provider)) {
            return groqEnabled && groqApiKey != null && !groqApiKey.isBlank();
        }
        return "LOCAL_RULES".equals(provider);
    }

    public boolean isCircuitOpen(String provider) {
        Instant until = circuitOpenUntil.get(provider);
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            circuitOpenUntil.remove(provider);
            consecutiveFailures.put(provider, 0);
            return false;
        }
        return true;
    }

    @Transactional
    public boolean tryAcquire(String provider, Long userId, long estimatedTokens) {
        if (!isProviderAuthorizedAndEnabled(provider) || isCircuitOpen(provider)) {
            return false;
        }

        if ("LOCAL_RULES".equals(provider)) {
            return true;
        }

        if (!getSemaphore().tryAcquire()) {
            return false;
        }

        boolean acquired = false;
        try {
            Instant now = Instant.now();
            Instant oneMinuteAgo = now.minusSeconds(60);

            pruneOld(globalMinuteWindow, oneMinuteAgo);
            if (globalMinuteWindow.size() >= globalRpmLimit) {
                return false;
            }

            pruneOldTokens(globalTokenMinuteWindow, oneMinuteAgo);
            long tokensLastMinute = globalTokenMinuteWindow.stream().mapToLong(TokenWindowEntry::tokens).sum();
            long effectiveTpm = (long) Math.floor(tpmLimit * (100.0 - reservePercent) / 100.0);
            if ((tokensLastMinute + estimatedTokens) > effectiveTpm) {
                return false;
            }

            Deque<Instant> userWindow = userMinuteWindows.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());
            pruneOld(userWindow, oneMinuteAgo);
            if (userWindow.size() >= userRpmLimit) {
                return false;
            }

            LocalDate hoy = LocalDate.now();
            var consumosHoy = consumoRecursoRepository.findByProveedorAndFecha(provider, hoy);
            int globalRequestsToday = consumosHoy.stream().mapToInt(ConsumoRecurso::getSolicitudes).sum();
            long globalTokensToday = consumosHoy.stream().mapToLong(ConsumoRecurso::getTokensEstimados).sum();

            int effectiveGlobalRpd = (int) Math.floor(globalRpdLimit * (100.0 - reservePercent) / 100.0);
            long effectiveTokenBudget = (long) Math.floor(dailyTokenBudget * (100.0 - reservePercent) / 100.0);

            if (globalRequestsToday >= effectiveGlobalRpd || (globalTokensToday + estimatedTokens) > effectiveTokenBudget) {
                return false;
            }

            ConsumoRecurso userConsumo = consumoRecursoRepository
                    .findByProveedorAndUsuarioIdAndFecha(provider, userId, hoy)
                    .orElseGet(() -> new ConsumoRecurso(provider, userId, hoy));

            if (userConsumo.getSolicitudes() >= userRpdLimit) {
                return false;
            }

            globalMinuteWindow.addLast(now);
            globalTokenMinuteWindow.addLast(new TokenWindowEntry(now, estimatedTokens));
            userWindow.addLast(now);
            userConsumo.setSolicitudes(userConsumo.getSolicitudes() + 1);
            userConsumo.setTokensEstimados(userConsumo.getTokensEstimados() + estimatedTokens);
            consumoRecursoRepository.save(userConsumo);

            acquired = true;
            return true;
        } finally {
            if (!acquired) {
                getSemaphore().release();
            }
        }
    }

    public void release(String provider) {
        if (!"LOCAL_RULES".equals(provider)) {
            getSemaphore().release();
        }
    }

    public void recordSuccess(String provider) {
        consecutiveFailures.put(provider, 0);
    }

    public void recordFailure(String provider, int httpStatus) {
        // 429 (cuota) o 401/403 (auth) abren el circuit breaker de inmediato sin reintentos
        if (httpStatus == 429 || httpStatus == 401 || httpStatus == 403) {
            circuitOpenUntil.put(provider, Instant.now().plusSeconds(circuitOpenSeconds));
            return;
        }
        int count = consecutiveFailures.merge(provider, 1, Integer::sum);
        if (count >= circuitFailureThreshold) {
            circuitOpenUntil.put(provider, Instant.now().plusSeconds(circuitOpenSeconds));
        }
    }

    private void pruneOld(Deque<Instant> deque, Instant cutoff) {
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }

    private void pruneOldTokens(Deque<TokenWindowEntry> deque, Instant cutoff) {
        while (!deque.isEmpty() && deque.peekFirst().timestamp().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }
}
