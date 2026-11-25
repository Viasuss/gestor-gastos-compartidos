package com.gestorgastos.gestor_gastos.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parser estricto de montos monetarios en pesos colombianos (COP, enteros).
 *
 * Rechaza valores nulos, vacios, negativos, caracteres alfabeticos,
 * separadores incorrectos o decimales ambiguos sin transformarlos silenciosamente.
 */
public final class MoneyParser {

    private static final Pattern PLAIN_DIGITS = Pattern.compile("^\\$?\\s*\\d+$");
    private static final Pattern GROUPED_THOUSANDS = Pattern.compile("^\\$?\\s*\\d{1,3}(\\.\\d{3})+$");
    private static final Pattern EXACT_ZERO_DECIMALS = Pattern.compile("^\\$?\\s*\\d+\\.0{1,2}$");

    private MoneyParser() {
    }

    public static Optional<BigDecimal> parseCop(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        if (PLAIN_DIGITS.matcher(trimmed).matches() || GROUPED_THOUSANDS.matcher(trimmed).matches()) {
            String digits = trimmed.replace("$", "").replace(".", "").trim();
            if (digits.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new BigDecimal(digits));
        }

        return Optional.empty();
    }

    /**
     * Acepta adicionalmente representacion decimal interna exacta (ej. "60000.00" de DECIMAL(19,2))
     * para formularios prellenados desde BD, rechazando cualquier fraccion distinta de cero.
     */
    public static Optional<BigDecimal> parseCopAcceptingZeroDecimals(String raw) {
        if (raw != null) {
            String trimmed = raw.trim();
            if (EXACT_ZERO_DECIMALS.matcher(trimmed).matches()) {
                String integerPart = trimmed.substring(0, trimmed.indexOf('.')).replace("$", "").trim();
                return integerPart.isEmpty() ? Optional.empty() : Optional.of(new BigDecimal(integerPart));
            }
        }
        return parseCop(raw);
    }
}
