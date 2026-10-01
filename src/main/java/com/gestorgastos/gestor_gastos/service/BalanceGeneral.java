package com.gestorgastos.gestor_gastos.service;

import java.math.BigDecimal;

// NUEVO: contenedor simple para el resultado de calcularBalanceGeneral().
// Guarda los dos números por separado (lo que te deben en total, y lo
// que debes en total, sumando TODOS los grupos del usuario), tal como
// los muestra la tarjeta "BALANCE TOTAL" del Figma.
public class BalanceGeneral {

    private BigDecimal totalLeDeben;
    private BigDecimal totalDebe;

    public BalanceGeneral(BigDecimal totalLeDeben, BigDecimal totalDebe) {
        this.totalLeDeben = totalLeDeben;
        this.totalDebe = totalDebe;
    }

    public BigDecimal getTotalLeDeben() {
        return totalLeDeben;
    }

    public BigDecimal getTotalDebe() {
        return totalDebe;
    }
}