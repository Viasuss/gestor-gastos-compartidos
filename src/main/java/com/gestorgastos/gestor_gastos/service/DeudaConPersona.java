package com.gestorgastos.gestor_gastos.service;

import com.gestorgastos.gestor_gastos.entity.Usuario;

import java.math.BigDecimal;

// Representa UNA línea del resumen de deudas de un usuario dentro de un grupo.
// Ejemplos de lo que puede representar un objeto de esta clase:
//   - "Le debes $30.000 a Johan"   -> monto negativo (tú debes)
//   - "Ana te debe $15.000"        -> monto positivo (a ti te deben)
//
// Usamos un solo campo "monto" con signo (positivo o negativo) en vez de
// dos campos separados ("debo" y "meDeben"), porque simplifica el cálculo:
// vamos sumando y restando sobre el mismo número a medida que revisamos
// gasto por gasto, y al final el signo nos dice la dirección de la deuda.
public class DeudaConPersona {

    private Usuario otraPersona;
    private BigDecimal monto;

    public DeudaConPersona(Usuario otraPersona, BigDecimal monto) {
        this.otraPersona = otraPersona;
        this.monto = monto;
    }

    public Usuario getOtraPersona() {
        return otraPersona;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void sumarMonto(BigDecimal cantidad) {
        this.monto = this.monto.add(cantidad);
    }

    // true si monto > 0: significa que ESA persona te debe a ti
    public boolean meDeben() {
        return monto.compareTo(BigDecimal.ZERO) > 0;
    }

    // true si monto < 0: significa que TÚ le debes a esa persona
    public boolean leDebo() {
        return monto.compareTo(BigDecimal.ZERO) < 0;
    }
}