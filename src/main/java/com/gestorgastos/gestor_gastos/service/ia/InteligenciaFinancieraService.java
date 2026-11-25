package com.gestorgastos.gestor_gastos.service.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Motor de interpretacion financiera (B3).
 *
 * Orden de resolucion:
 * 1. Reglas locales deterministas (si son claras y completas).
 * 2. Groq Free (si esta habilitado, autorizado y dentro de cuota en ResourceManager).
 * 3. Solicitud de aclaracion o fallback manual.
 */
@Service
public class InteligenciaFinancieraService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    private final LocalRulesProvider localRulesProvider;
    private final GroqProvider groqProvider;

    @Value("${nexus.ai.groq.api-key:}")
    private String groqApiKey;

    public InteligenciaFinancieraService(
            LocalRulesProvider localRulesProvider,
            GroqProvider groqProvider) {
        this.localRulesProvider = localRulesProvider;
        this.groqProvider = groqProvider;
    }

    public InterpretacionGastoResponse interpretarGasto(InterpretacionGastoRequest request, Long usuarioId) {
        Optional<InterpretacionGastoResponse> localOpt = localRulesProvider.interpretar(request);

        if (localOpt.isPresent() && !localOpt.get().requiereAclaracion()) {
            return localOpt.get();
        }

        if (groqProvider.isAvailable()) {
            Optional<InterpretacionGastoResponse> groqOpt =
                    groqProvider.interpretarConUsuario(request, usuarioId, groqApiKey);
            if (groqOpt.isPresent()) {
                return groqOpt.get();
            }
        }

        return localOpt.orElseGet(() -> new InterpretacionGastoResponse(
                "REGISTRO_GASTO",
                "Gasto compartido",
                null,
                "Otros",
                request.fechaReferencia() != null ? request.fechaReferencia() : LocalDate.now(BUSINESS_ZONE),
                null,
                List.of(),
                null,
                "IGUALITARIA",
                true,
                List.of("montoCop", "concepto", "participantes"),
                "MANUAL",
                "No fue posible interpretar automáticamente; completa los campos manualmente."
        ));
    }
}
