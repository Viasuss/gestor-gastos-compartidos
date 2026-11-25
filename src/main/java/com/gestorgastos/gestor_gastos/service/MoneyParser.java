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
}
