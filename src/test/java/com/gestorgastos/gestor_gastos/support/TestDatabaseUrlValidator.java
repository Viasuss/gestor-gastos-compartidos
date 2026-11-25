package com.gestorgastos.gestor_gastos.support;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validador estricto y determinista de URLs JDBC para el entorno de pruebas (C1 + C6 / F11A-AUD-003).
 *
 * Politica de conjunto cerrado:
 * - Destino exclusivo H2 en memoria: ^jdbc:h2:mem:[a-zA-Z0-9_-]+$
 * - Parametros permitidos (sin duplicados ni contradicciones):
 *   MODE=MySQL, DB_CLOSE_DELAY=-1, DB_CLOSE_ON_EXIT=FALSE
 * - Rechaza URLs nulas/vacias, MySQL real, H2 en disco/TCP, parametros repetidos,
 *   contradictorios, desconocidos o mecanismos de inicializacion SQL (INIT, RUNSCRIPT).
 */
public final class TestDatabaseUrlValidator {

    private static final Pattern BASE_H2_MEMORY_PATTERN =
            Pattern.compile("^jdbc:h2:mem:[a-zA-Z0-9_-]+$");

    private static final Map<String, String> ALLOWED_PARAMETERS = Map.of(
            "MODE", "MySQL",
            "DB_CLOSE_DELAY", "-1",
            "DB_CLOSE_ON_EXIT", "FALSE"
    );

    private TestDatabaseUrlValidator() {
    }

    public static void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: spring.datasource.url es nula o vacia."
            );
        }

        if (url.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: La URL JDBC contiene espacios o caracteres de control."
            );
        }

        String[] segments = url.split(";", -1);
        String baseUrl = segments[0];

        if (!BASE_H2_MEMORY_PATTERN.matcher(baseUrl).matches()) {
            throw new IllegalStateException(
                    "AISLAMIENTO_PRUEBAS_BLOQUEADO: Destino JDBC no autorizado para pruebas: " + url
            );
        }

        Set<String> seenKeys = new HashSet<>();
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                throw new IllegalStateException(
                        "AISLAMIENTO_PRUEBAS_BLOQUEADO: Parametro vacio en URL JDBC de pruebas: " + url
                );
            }

            int eqIndex = segment.indexOf('=');
            if (eqIndex <= 0 || eqIndex == segment.length() - 1) {
                throw new IllegalStateException(
                        "AISLAMIENTO_PRUEBAS_BLOQUEADO: Parametro malformado en URL JDBC: " + segment
                );
            }

            String key = segment.substring(0, eqIndex);
            String value = segment.substring(eqIndex + 1);

            if (!seenKeys.add(key)) {
                throw new IllegalStateException(
                        "AISLAMIENTO_PRUEBAS_BLOQUEADO: Parametro duplicado o contradictorio en URL JDBC: " + key
                );
            }

            String expectedValue = ALLOWED_PARAMETERS.get(key);
            if (expectedValue == null || !expectedValue.equals(value)) {
                throw new IllegalStateException(
                        "AISLAMIENTO_PRUEBAS_BLOQUEADO: Parametro o valor no autorizado en URL JDBC: " + segment
                );
            }
        }
    }

    public static boolean isAllowed(String url) {
        try {
            validate(url);
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }
}
