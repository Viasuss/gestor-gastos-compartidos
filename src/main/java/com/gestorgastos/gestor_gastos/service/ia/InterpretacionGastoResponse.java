package com.gestorgastos.gestor_gastos.service.ia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InterpretacionGastoResponse(
        String tipoOperacion,
        String concepto,
        BigDecimal montoCop,
        String categoriaSugerida,
        LocalDate fechaSugerida,
        String pagadorSugerido,
        List<String> participantesSugeridos,
        Integer cantidadParticipantes,
        String tipoDivision,
        boolean requiereAclaracion,
        List<String> camposPendientes,
        String origenProveedor,
        String mensajeUsuario
) {
}
