package com.gestorgastos.gestor_gastos.support;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.Set;

/**
 * Guardia temprana de aislamiento de pruebas (C2 + C7).
 *
 * Se ejecuta durante ApplicationEnvironmentPreparedEvent (antes de crear DataSource,
 * inicializar HikariCP, arrancar Hibernate o ejecutar DDL).
 */
public class TestIsolationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String EXPECTED_TEST_USERNAME = "partly_test";
    private static final Set<String> ALLOWED_DDL_MODES = Set.of("create-drop", "none", "validate");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        validateEnvironment(environment);
    }

    public static void validateEnvironment(ConfigurableEnvironment environment) {
        boolean testProfileActive = Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (!testProfileActive) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: El perfil 'test' debe estar activo para ejecutar pruebas."
            );
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url");
        TestDatabaseUrlValidator.validate(datasourceUrl);

        String hikariJdbcUrl = environment.getProperty("spring.datasource.hikari.jdbc-url");
        if (hikariJdbcUrl != null && !hikariJdbcUrl.isBlank()) {
            TestDatabaseUrlValidator.validate(hikariJdbcUrl);
        }

        String jndiName = environment.getProperty("spring.datasource.jndi-name");
        if (jndiName != null && !jndiName.isBlank()) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: No se permite spring.datasource.jndi-name en pruebas."
            );
        }

        String driver = environment.getProperty("spring.datasource.driver-class-name");
        if (driver != null && !driver.isBlank() && !"org.h2.Driver".equals(driver)) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: Driver no autorizado en pruebas: " + driver
            );
        }

        String username = environment.getProperty("spring.datasource.username");
        if (!EXPECTED_TEST_USERNAME.equals(username)) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: El usuario de BD en pruebas debe ser '"
                            + EXPECTED_TEST_USERNAME + "', pero fue: " + username
            );
        }

        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto != null && !ddlAuto.isBlank() && !ALLOWED_DDL_MODES.contains(ddlAuto)) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: spring.jpa.hibernate.ddl-auto no autorizado en pruebas: " + ddlAuto
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
