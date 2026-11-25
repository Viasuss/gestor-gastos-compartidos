package com.gestorgastos.gestor_gastos.service.ia;

import com.gestorgastos.gestor_gastos.repository.ConsumoRecursoRepository;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
class ResourceManagerAndIaTest {

    @Autowired private ResourceManager resourceManager;
    @Autowired private ConsumoRecursoRepository consumoRecursoRepository;
    @Autowired private LocalRulesProvider localRulesProvider;
    @Autowired private GroqProvider groqProvider;
    @Autowired private InteligenciaFinancieraService iaService;

    @BeforeEach
    void setUp() {
        consumoRecursoRepository.deleteAll();
    }

    @Test
    void localRulesInterpretaExpresionesColombianasYDetectaAmbiguedad() {
        InterpretacionGastoRequest claro = new InterpretacionGastoRequest(
                1L, "Pagué 120 lucas del internet del apartamento. Divídelo entre todos",
                3, List.of("Ana", "Beto", "Carla"), LocalDate.of(2026, 11, 25));

        InterpretacionGastoResponse resp = localRulesProvider.interpretar(claro).orElseThrow();
        assertEquals(new BigDecimal("120000"), resp.montoCop());
        assertEquals("Servicios", resp.categoriaSugerida());
        assertFalse(resp.requiereAclaracion());
        assertEquals(3, resp.participantesSugeridos().size());

        // "entre tres" cuando hay 5 miembros activos -> pide aclaracion, no adivina participantes
        InterpretacionGastoRequest ambiguoParticipantes = new InterpretacionGastoRequest(
                1L, "Compré 50 mil de mercado ayer entre tres",
                5, List.of("Ana", "Beto", "Carla", "Dani", "Eva"), LocalDate.of(2026, 11, 25));
        InterpretacionGastoResponse respAmb = localRulesProvider.interpretar(ambiguoParticipantes).orElseThrow();
        assertEquals(new BigDecimal("50000"), respAmb.montoCop());
        assertEquals(LocalDate.of(2026, 11, 24), respAmb.fechaSugerida());
        assertTrue(respAmb.requiereAclaracion());
        assertTrue(respAmb.camposPendientes().contains("participantes"));
    }

    @Test
    void groqProviderValidaJsonEstrictamenteYRechazaCamposInesperadosOMaliciosos() {
        String validJson = """
                {"choices":[{"message":{"content":"{\\"tipoOperacion\\":\\"REGISTRO_GASTO\\",\\"concepto\\":\\"Internet\\",\\"montoCop\\":120000,\\"categoriaSugerida\\":\\"Servicios\\",\\"fechaSugerida\\":\\"2026-11-25\\",\\"participantesSugeridos\\":[],\\"cantidadParticipantes\\":3,\\"tipoDivision\\":\\"IGUALITARIA\\",\\"requiereAclaracion\\":true,\\"camposPendientes\\":[\\"participantes\\"]}"}}]}
                """;
        Optional<InterpretacionGastoResponse> ok = groqProvider.parseAndValidateResponse(validJson);
        assertTrue(ok.isPresent());
        assertEquals(new BigDecimal("120000"), ok.get().montoCop());

        // Rechaza campos inesperados (ej. inyeccion de campo sql/admin)
        String maliciousJson = """
                {"choices":[{"message":{"content":"{\\"tipoOperacion\\":\\"REGISTRO_GASTO\\",\\"concepto\\":\\"Hack\\",\\"montoCop\\":1000,\\"executeSql\\":\\"DROP TABLE usuario\\"}"}}]}
                """;
        assertTrue(groqProvider.parseAndValidateResponse(maliciousJson).isEmpty());

        // Rechaza monto negativo
        String negativeJson = """
                {"choices":[{"message":{"content":"{\\"tipoOperacion\\":\\"REGISTRO_GASTO\\",\\"concepto\\":\\"Neg\\",\\"montoCop\\":-5000}"}}]}
                """;
        assertTrue(groqProvider.parseAndValidateResponse(negativeJson).isEmpty());
    }

    @Test
    void resourceManagerBloqueaGroqSinApiKeyYAplicaCircuitBreakerEn429() {
        assertFalse(resourceManager.isProviderAuthorizedAndEnabled("GROQ"));
        assertFalse(resourceManager.tryAcquire("GROQ", 1L, 500L));

        // Sin Groq, InteligenciaFinancieraService hace fallback seguro a LOCAL_RULES o MANUAL
        InterpretacionGastoRequest req = new InterpretacionGastoRequest(
                1L, "Un millón de arriendo entre todos",
                2, List.of("Ana", "Beto"), LocalDate.of(2026, 11, 25));
        InterpretacionGastoResponse resp = iaService.interpretarGasto(req, 1L);
        assertEquals("LOCAL_RULES", resp.origenProveedor());
        assertEquals(new BigDecimal("1000000"), resp.montoCop());

        // Simular circuit breaker ante HTTP 429
        ReflectionTestUtils.setField(resourceManager, "groqEnabled", true);
        ReflectionTestUtils.setField(resourceManager, "groqApiKey", "synthetic-test-key");
        try {
            assertTrue(resourceManager.isProviderAuthorizedAndEnabled("GROQ"));
            resourceManager.recordFailure("GROQ", 429);
            assertTrue(resourceManager.isCircuitOpen("GROQ"));
            assertFalse(resourceManager.tryAcquire("GROQ", 1L, 500L));
        } finally {
            ReflectionTestUtils.setField(resourceManager, "groqEnabled", false);
            ReflectionTestUtils.setField(resourceManager, "groqApiKey", "");
        }
    }
}
