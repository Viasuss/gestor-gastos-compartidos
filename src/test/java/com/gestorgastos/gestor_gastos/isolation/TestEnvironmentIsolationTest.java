package com.gestorgastos.gestor_gastos.isolation;

import com.gestorgastos.gestor_gastos.service.GoogleAuthService;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import com.gestorgastos.gestor_gastos.support.TestDatabaseUrlValidator;
import com.gestorgastos.gestor_gastos.support.TestIsolationEnvironmentPostProcessor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas de verificacion del aislamiento del entorno de pruebas (F1.1.A + F1.1.A.2).
 * Demuestran cumplimiento de las correcciones C1, C2, C3, C4, C6 y C7.
 */
class TestEnvironmentIsolationTest {

    @Nested
    class ValidacionEstrictaUrlYEntornoTests {

        @Test
        void aceptaUrlH2EnMemoriaConParametrosAutorizadosSinDuplicados() {
            String urlAutorizada = "jdbc:h2:mem:partly_test_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            assertDoesNotThrow(() -> TestDatabaseUrlValidator.validate(urlAutorizada));
            assertTrue(TestDatabaseUrlValidator.isAllowed(urlAutorizada));
            assertTrue(TestDatabaseUrlValidator.isAllowed("jdbc:h2:mem:otra_bd_test"));
        }

        @Test
        void rechazaUrlMysqlRealH2NoEnMemoriaYParametrosNoAutorizados() {
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:mysql://localhost:3306/gestor_gastos"));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:file:./data/partly"));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:~/gestor_gastos"));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:tcp://localhost/~/test"));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:mem:partly_test_db;INIT=RUNSCRIPT FROM 'evil.sql'"));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:mem:partly_test_db;TRACE_LEVEL_SYSTEM_OUT=2"));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate(""));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate(null));
        }

        @Test
        void rechazaParametrosDuplicadosContradictoriosYMalformadosEnUrl() {
            // Parametro duplicado
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:mem:partly_test_db;MODE=MySQL;MODE=MySQL"));
            // Parametro contradictorio
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:mem:partly_test_db;MODE=MySQL;MODE=PostgreSQL"));
            // Valor no autorizado para parametro conocido
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:mem:partly_test_db;DB_CLOSE_DELAY=0"));
            // Segmento vacio o malformado
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:mem:partly_test_db;;MODE=MySQL"));
            assertThrows(IllegalStateException.class, () ->
                    TestDatabaseUrlValidator.validate("jdbc:h2:mem:partly_test_db;MODE"));
        }

        @Test
        void guardiaTempranaRechazaConfiguracionesConflictivasYUsuarioNoSintetico() {
            MockEnvironment sinPerfilTest = new MockEnvironment()
                    .withProperty("spring.datasource.url", "jdbc:h2:mem:partly_test_db;MODE=MySQL");
            assertThrows(IllegalStateException.class, () ->
                    TestIsolationEnvironmentPostProcessor.validateEnvironment(sinPerfilTest));

            MockEnvironment conMysqlEnUrl = new MockEnvironment()
                    .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/gestor_gastos")
                    .withProperty("spring.datasource.username", "partly_test")
                    .withProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
            conMysqlEnUrl.setActiveProfiles("test");
            assertThrows(IllegalStateException.class, () ->
                    TestIsolationEnvironmentPostProcessor.validateEnvironment(conMysqlEnUrl));

            MockEnvironment conHikariUrlNoAutorizada = new MockEnvironment()
                    .withProperty("spring.datasource.url", "jdbc:h2:mem:partly_test_db;MODE=MySQL")
                    .withProperty("spring.datasource.hikari.jdbc-url", "jdbc:mysql://localhost:3306/gestor_gastos")
                    .withProperty("spring.datasource.username", "partly_test")
                    .withProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
            conHikariUrlNoAutorizada.setActiveProfiles("test");
            assertThrows(IllegalStateException.class, () ->
                    TestIsolationEnvironmentPostProcessor.validateEnvironment(conHikariUrlNoAutorizada));

            MockEnvironment conUsuarioRoot = new MockEnvironment()
                    .withProperty("spring.datasource.url", "jdbc:h2:mem:partly_test_db;MODE=MySQL")
                    .withProperty("spring.datasource.username", "root")
                    .withProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
            conUsuarioRoot.setActiveProfiles("test");
            assertThrows(IllegalStateException.class, () ->
                    TestIsolationEnvironmentPostProcessor.validateEnvironment(conUsuarioRoot));

            MockEnvironment conOtroUsuarioNoAutorizado = new MockEnvironment()
                    .withProperty("spring.datasource.url", "jdbc:h2:mem:partly_test_db;MODE=MySQL")
                    .withProperty("spring.datasource.username", "admin_prod")
                    .withProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
            conOtroUsuarioNoAutorizado.setActiveProfiles("test");
            assertThrows(IllegalStateException.class, () ->
                    TestIsolationEnvironmentPostProcessor.validateEnvironment(conOtroUsuarioNoAutorizado));

            MockEnvironment conDdlUpdate = new MockEnvironment()
                    .withProperty("spring.datasource.url", "jdbc:h2:mem:partly_test_db;MODE=MySQL")
                    .withProperty("spring.datasource.username", "partly_test")
                    .withProperty("spring.jpa.hibernate.ddl-auto", "update");
            conDdlUpdate.setActiveProfiles("test");
            assertThrows(IllegalStateException.class, () ->
                    TestIsolationEnvironmentPostProcessor.validateEnvironment(conDdlUpdate));
        }

        @Test
        void googleAuthServiceCortocircuitaConPlaceholderSinLlamarRed() {
            GoogleAuthService realService = new GoogleAuthService();
            RestTemplate restTemplateGuard = Mockito.mock(RestTemplate.class);
            when(restTemplateGuard.getForObject(anyString(), any()))
                    .thenThrow(new AssertionError("Prohibido realizar llamadas HTTP reales en pruebas"));

            ReflectionTestUtils.setField(realService, "restTemplate", restTemplateGuard);
            ReflectionTestUtils.setField(realService, "googleClientId", "YOUR_GOOGLE_CLIENT_ID_TEST_DISABLED");

            Optional<GoogleAuthService.GoogleUserInfo> resultado = realService.verificarToken("token.simulado.jwt");

            assertTrue(resultado.isEmpty());
            verifyNoInteractions(restTemplateGuard);
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(ExternalProvidersTestConfig.class)
    class ContextoSpringAisladoTests {

        @Autowired
        private Environment environment;

        @Autowired
        private DataSource dataSource;

        @Autowired
        private GoogleAuthService googleAuthService;

        @Test
        void contextoUsaPerfilTestH2EnMemoriaYCredencialesSinteticas() throws Exception {
            assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("test"));
            assertEquals("YOUR_GOOGLE_CLIENT_ID_TEST_DISABLED", environment.getProperty("google.client.id"));

            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                String jdbcUrl = meta.getURL();
                assertTrue(jdbcUrl.startsWith("jdbc:h2:mem:"),
                        "Se esperaba H2 en memoria pero se obtuvo: " + jdbcUrl);
                assertEquals("PARTLY_TEST", meta.getUserName().toUpperCase());
            }
        }

        @Test
        void googleAuthServiceEstaSimuladoSinLlamadasExternas() {
            assertTrue(Mockito.mockingDetails(googleAuthService).isMock(),
                    "GoogleAuthService debe estar simulado en pruebas de contexto (C3)");
            assertEquals(Optional.empty(), googleAuthService.verificarToken("token-cualquiera"));
            assertEquals("TEST_MOCK_GOOGLE_CLIENT_ID", googleAuthService.getGoogleClientId());
        }
    }
}
