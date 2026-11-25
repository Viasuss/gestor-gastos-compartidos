package com.gestorgastos.gestor_gastos.service.ia;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gestorgastos.gestor_gastos.repository.ConsumoRecursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqProviderAndTelemetryTest {

    private ResourceManager resourceManager;
    private GroqProvider groqProvider;
    private MockRestServiceServer mockServer;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        ConsumoRecursoRepository repo = mock(ConsumoRecursoRepository.class);
        when(repo.findByProveedorAndFecha(any(), any())).thenReturn(java.util.List.of());
        when(repo.findByProveedorAndUsuarioIdAndFecha(any(), any(), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        resourceManager = new ResourceManager(repo);
        ReflectionTestUtils.setField(resourceManager, "groqEnabled", true);
        ReflectionTestUtils.setField(resourceManager, "groqApiKey", "gsk_test_secret_never_log_123");
        ReflectionTestUtils.setField(resourceManager, "globalRpmLimit", 20);
        ReflectionTestUtils.setField(resourceManager, "userRpmLimit", 5);
        ReflectionTestUtils.setField(resourceManager, "globalRpdLimit", 500);
        ReflectionTestUtils.setField(resourceManager, "userRpdLimit", 50);
        ReflectionTestUtils.setField(resourceManager, "dailyTokenBudget", 150000L);
        ReflectionTestUtils.setField(resourceManager, "tpmLimit", 6000L);
        ReflectionTestUtils.setField(resourceManager, "reservePercent", 10);
        ReflectionTestUtils.setField(resourceManager, "maxConcurrent", 2);
        ReflectionTestUtils.setField(resourceManager, "circuitFailureThreshold", 3);
        ReflectionTestUtils.setField(resourceManager, "circuitOpenSeconds", 60L);

        groqProvider = new GroqProvider(resourceManager);
        ReflectionTestUtils.setField(groqProvider, "model", "openai/gpt-oss-20b");
        RestTemplate rt = (RestTemplate) ReflectionTestUtils.getField(groqProvider, "restTemplate");
        mockServer = MockRestServiceServer.createServer(rt);

        Logger logger = (Logger) LoggerFactory.getLogger(GroqProvider.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    private InterpretacionGastoRequest sampleRequest() {
        return new InterpretacionGastoRequest(1L, "Almuerzo equipo", 3, java.util.List.of("A", "B", "C"), LocalDate.of(2026, 11, 25));
    }

    @Test
    void solicitudExitosaRetornaGroqYNoExponeSecretosEnLogs() {
        String groqResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"tipoOperacion\\":\\"REGISTRO_GASTO\\",\\"concepto\\":\\"Almuerzo equipo\\",\\"montoCop\\":85000,\\"categoriaSugerida\\":\\"Alimentacion\\",\\"fechaSugerida\\":\\"2026-11-25\\",\\"participantesSugeridos\\":[],\\"cantidadParticipantes\\":3,\\"tipoDivision\\":\\"IGUALITARIA\\",\\"requiereAclaracion\\":false,\\"camposPendientes\\":[]}"
                      }
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-20b"))
                .andExpect(jsonPath("$.max_completion_tokens").value(800))
                .andExpect(jsonPath("$.reasoning_format").value("hidden"))
                .andExpect(jsonPath("$.reasoning_effort").value("low"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.max_tokens").doesNotExist())
                .andRespond(withSuccess(groqResponse, MediaType.APPLICATION_JSON));

        Optional<InterpretacionGastoResponse> res = groqProvider.interpretarConUsuario(
                sampleRequest(), 1L, "gsk_test_secret_never_log_123");

        assertTrue(res.isPresent());
        assertEquals("GROQ", res.get().origenProveedor());
        assertEquals(0, new BigDecimal("85000").compareTo(res.get().montoCop()));
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_HTTP_SUCCESS")));
        assertTrue(logAppender.list.stream().noneMatch(e -> e.getFormattedMessage().contains("gsk_test_secret")));
    }

    @Test
    void manejaErroresHttp400_401_403_429_500_Timeout_Y_JsonInvalido() {
        // 400 con detalle seguro
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"type\":\"invalid_request_error\",\"code\":\"unsupported_parameter\",\"param\":\"reasoning_format\",\"message\":\"Invalid parameter\"}}"));
        assertTrue(groqProvider.interpretarConUsuario(sampleRequest(), 1L, "gsk_test_secret_never_log_123").isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e ->
                e.getFormattedMessage().contains("GROQ_HTTP_ERROR")
                        && e.getFormattedMessage().contains("status=400")
                        && e.getFormattedMessage().contains("errorCode=unsupported_parameter")));

        // JSON invalido -> GROQ_JSON_PARSE_ERROR
        mockServer.reset();
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"not-json\"}}]}", MediaType.APPLICATION_JSON));
        assertTrue(groqProvider.interpretarConUsuario(sampleRequest(), 1L, "gsk_test_secret_never_log_123").isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_JSON_PARSE_ERROR")));

        // 429 abre circuit breaker
        mockServer.reset();
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertTrue(groqProvider.interpretarConUsuario(sampleRequest(), 1L, "gsk_test_secret_never_log_123").isEmpty());
        assertTrue(resourceManager.isCircuitOpen("GROQ"));

        // Siguiente llamada rechazada por CIRCUIT_BREAKER_OPEN sin tocar HTTP
        assertTrue(groqProvider.interpretarConUsuario(sampleRequest(), 1L, "gsk_test_secret_never_log_123").isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("CIRCUIT_BREAKER_OPEN")));
    }

    @Test
    void manejaTimeoutDeshabilitadoYApiKeyFaltante() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(req -> { throw new ResourceAccessException("Read timed out"); });
        assertTrue(groqProvider.interpretarConUsuario(sampleRequest(), 1L, "gsk_test_secret_never_log_123").isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_TIMEOUT")));

        ReflectionTestUtils.setField(resourceManager, "groqEnabled", false);
        assertTrue(groqProvider.interpretarConUsuario(sampleRequest(), 1L, "gsk_test_secret_never_log_123").isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_DISABLED")));

        assertTrue(groqProvider.interpretarConUsuario(sampleRequest(), 1L, "").isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("API_KEY_MISSING")));
    }

    @Test
    void telemetriaGranularParaMontoNuloChoicesVacioContentVacioYTruncado() {
        // 1. Monto nulo con aclaracion requerida -> aceptado con GROQ_CLARIFICATION_REQUIRED
        String nullMontoJson = """
                {
                  "choices": [{
                    "finish_reason": "stop",
                    "message": {
                      "content": "{\\"tipoOperacion\\":\\"REGISTRO_GASTO\\",\\"concepto\\":\\"Insumos apartamento\\",\\"montoCop\\":null,\\"requiereAclaracion\\":true,\\"camposPendientes\\":[\\"montoCop\\"]}"
                    }
                  }],
                  "usage": {"prompt_tokens": 120, "completion_tokens": 45}
                }
                """;
        Optional<InterpretacionGastoResponse> acl = groqProvider.parseAndValidateResponse(nullMontoJson, "c1", 200, 300L);
        assertTrue(acl.isPresent());
        assertNull(acl.get().montoCop());
        assertTrue(acl.get().requiereAclaracion());
        assertEquals("GROQ", acl.get().origenProveedor());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_CLARIFICATION_REQUIRED")));

        // 2. Choices vacio
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[]}", "c2", 200, 100L).isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_CHOICES_EMPTY")));

        // 3. finish_reason=length
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{\"}}]}", "c3", 200, 100L).isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_COMPLETION_TRUNCATED")));

        // 4. Content vacio
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"\"}}]}", "c4", 200, 100L).isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_CONTENT_EMPTY")));

        // 5. Validacion semantica fallida (monto <= 0)
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"tipoOperacion\\\":\\\"REGISTRO_GASTO\\\",\\\"montoCop\\\":0}\"}}]}", "c5", 200, 100L).isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("GROQ_SEMANTIC_VALIDATION_FAILED")));
    }

    @Test
    void rechazaTipoOperacionNuloVacioUnsupportedYNormalizaEspaciosMayusculas() {
        // Nulo / ausente -> TIPO_OPERACION_MISSING
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"concepto\\\":\\\"Cena\\\"}\"}}]}", "t1", 200, 50L).isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("TIPO_OPERACION_MISSING")));

        // Vacio -> TIPO_OPERACION_EMPTY
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"tipoOperacion\\\":\\\"   \\\"}\"}}]}", "t2", 200, 50L).isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("TIPO_OPERACION_EMPTY")));

        // REQUIERE_ACLARACION o NO_FINANCIERO como tipoOperacion -> TIPO_OPERACION_UNSUPPORTED
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"tipoOperacion\\\":\\\"REQUIERE_ACLARACION\\\"}\"}}]}", "t3", 200, 50L).isEmpty());
        assertTrue(groqProvider.parseAndValidateResponse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"tipoOperacion\\\":\\\"NO_FINANCIERO\\\"}\"}}]}", "t4", 200, 50L).isEmpty());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("TIPO_OPERACION_UNSUPPORTED")));

        // Normalizacion segura de espacios y minusculas -> TIPO_OPERACION_NORMALIZED + TIPO_OPERACION_ACCEPTED
        Optional<InterpretacionGastoResponse> norm = groqProvider.parseAndValidateResponse(
                "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"tipoOperacion\\\":\\\" registro_gasto \\\",\\\"concepto\\\":\\\"Insumos\\\",\\\"montoCop\\\":null,\\\"requiereAclaracion\\\":true}\"}}]}",
                "t5", 200, 50L);
        assertTrue(norm.isPresent());
        assertEquals("GROQ", norm.get().origenProveedor());
        assertNull(norm.get().montoCop());
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("TIPO_OPERACION_NORMALIZED")));
        assertTrue(logAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("TIPO_OPERACION_ACCEPTED")));
    }
}
