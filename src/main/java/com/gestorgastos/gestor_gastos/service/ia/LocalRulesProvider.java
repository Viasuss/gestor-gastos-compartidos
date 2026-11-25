package com.gestorgastos.gestor_gastos.service.ia;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprete determinista basado en reglas locales para espanol colombiano (B3).
 */
@Component
public class LocalRulesProvider implements AiProvider {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999999999");

    private static final Pattern MILLON_PATTERN = Pattern.compile("\\b(un|1)\\s+mill[oó]n(?:es)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LUCAS_MIL_PATTERN = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)\\s*(?:lucas|mil|k)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PESOS_PATTERN = Pattern.compile("(?:\\$\\s*|\\b)(\\d{1,3}(?:\\.\\d{3})+|\\d{4,12})(?:\\s*pesos)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTRE_CANTIDAD_PATTERN = Pattern.compile("\\bentre\\s+(dos|tres|cuatro|cinco|seis|\\d+)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String getProviderName() {
        return "LOCAL_RULES";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<InterpretacionGastoResponse> interpretar(InterpretacionGastoRequest request) {
        if (request == null || request.texto() == null || request.texto().isBlank()) {
            return Optional.empty();
        }

        String texto = request.texto().trim();
        String lower = texto.toLowerCase(Locale.ROOT);

        List<BigDecimal> montosDetectados = extraerMontos(lower);
        List<String> camposPendientes = new ArrayList<>();
        BigDecimal montoCop = null;

        if (montosDetectados.size() == 1) {
            montoCop = montosDetectados.get(0);
            if (montoCop.signum() <= 0 || montoCop.compareTo(MONTO_MAXIMO) > 0) {
                montoCop = null;
                camposPendientes.add("montoCop");
            }
        } else {
            camposPendientes.add("montoCop");
        }

        LocalDate baseDate = request.fechaReferencia() != null ? request.fechaReferencia() : LocalDate.now(BUSINESS_ZONE);
        LocalDate fechaSugerida = lower.contains("ayer") ? baseDate.minusDays(1) : baseDate;

        List<String> participantesSugeridos = new ArrayList<>();
        Integer cantidadParticipantes = null;
        String tipoDivision = "IGUALITARIA";

        if (lower.contains("entre todos")) {
            if (request.nombresMiembros() != null && !request.nombresMiembros().isEmpty()) {
                participantesSugeridos.addAll(request.nombresMiembros());
                cantidadParticipantes = request.nombresMiembros().size();
            } else {
                camposPendientes.add("participantes");
            }
        } else {
            Matcher mEntre = ENTRE_CANTIDAD_PATTERN.matcher(lower);
            if (mEntre.find()) {
                int cant = parseCantidadTexto(mEntre.group(1));
                cantidadParticipantes = cant;
                if (request.cantidadMiembrosGrupo() == cant && request.nombresMiembros() != null) {
                    participantesSugeridos.addAll(request.nombresMiembros());
                } else {
                    // No asumir participantes especificos si la cantidad difiere del total del grupo
                    camposPendientes.add("participantes");
                }
            } else if (request.nombresMiembros() != null) {
                for (String nombre : request.nombresMiembros()) {
                    String primerNombre = nombre.split("\\s+")[0].toLowerCase(Locale.ROOT);
                    if (primerNombre.length() >= 3 && lower.contains(primerNombre)) {
                        participantesSugeridos.add(nombre);
                    }
                }
                if (participantesSugeridos.isEmpty()) {
                    camposPendientes.add("participantes");
                } else {
                    cantidadParticipantes = participantesSugeridos.size();
                }
            }
        }

        String concepto = extraerConcepto(texto);
        if (concepto.isBlank()) {
            camposPendientes.add("concepto");
        }

        String categoria = sugerirCategoria(lower);
        boolean requiereAclaracion = !camposPendientes.isEmpty();
        String mensaje = requiereAclaracion
                ? "Interpretación local parcial: revisa o completa " + String.join(", ", camposPendientes) + "."
                : "Interpretación local lista para revisión y confirmación.";

        return Optional.of(new InterpretacionGastoResponse(
                "REGISTRO_GASTO",
                concepto,
                montoCop,
                categoria,
                fechaSugerida,
                null,
                participantesSugeridos,
                cantidadParticipantes,
                tipoDivision,
                requiereAclaracion,
                camposPendientes,
                "LOCAL_RULES",
                mensaje
        ));
    }

    private List<BigDecimal> extraerMontos(String lower) {
        List<BigDecimal> montos = new ArrayList<>();
        Matcher mMillon = MILLON_PATTERN.matcher(lower);
        while (mMillon.find()) {
            montos.add(new BigDecimal("1000000"));
        }

        Matcher mLucas = LUCAS_MIL_PATTERN.matcher(lower);
        while (mLucas.find()) {
            String numStr = mLucas.group(1).replace(",", ".");
            try {
                BigDecimal val = new BigDecimal(numStr).multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP);
                montos.add(val);
            } catch (NumberFormatException ignored) {
            }
        }

        if (montos.isEmpty()) {
            Matcher mPesos = PESOS_PATTERN.matcher(lower);
            while (mPesos.find()) {
                String clean = mPesos.group(1).replace(".", "");
                try {
                    montos.add(new BigDecimal(clean));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return montos;
    }

    private int parseCantidadTexto(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "dos" -> 2;
            case "tres" -> 3;
            case "cuatro" -> 4;
            case "cinco" -> 5;
            case "seis" -> 6;
            default -> {
                try {
                    yield Integer.parseInt(token);
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
        };
    }

    private String extraerConcepto(String texto) {
        String limpio = texto
                .replaceAll("(?i)\\b(pagu[eé]|compr[eé]|gast[eé]|div[ií]delo|dividir|entre\\s+todos|entre\\s+\\w+|ayer|hoy)\\b", "")
                .replaceAll("(?i)\\b(un|1)\\s+mill[oó]n(?:es)?\\b", "")
                .replaceAll("(?i)\\b\\d+(?:[.,]\\d+)?\\s*(?:lucas|mil|k)\\b", "")
                .replaceAll("(?:\\$\\s*|\\b)\\d{1,3}(?:\\.\\d{3})+(?:\\s*pesos)?\\b", "")
                .replaceAll("\\b\\d{4,12}\\b", "")
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\s,.:;-]+|[\\s,.:;-]+$", "")
                .trim();
        if (limpio.length() > 100) {
            return limpio.substring(0, 100);
        }
        return limpio.isBlank() ? "Gasto compartido" : capitalize(limpio);
    }

    private String sugerirCategoria(String lower) {
        if (lower.contains("internet") || lower.contains("luz") || lower.contains("agua") || lower.contains("gas") || lower.contains("arriendo")) {
            return "Servicios";
        }
        if (lower.contains("mercado") || lower.contains("super") || lower.contains("d1") || lower.contains("ara")) {
            return "Mercado";
        }
        if (lower.contains("almuerzo") || lower.contains("cena") || lower.contains("comida") || lower.contains("cafe")) {
            return "Alimentación";
        }
        if (lower.contains("uber") || lower.contains("taxi") || lower.contains("bus") || lower.contains("gasolina")) {
            return "Transporte";
        }
        return "Otros";
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
