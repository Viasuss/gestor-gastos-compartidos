package com.gestorgastos.gestor_gastos.b6;

import com.gestorgastos.gestor_gastos.service.MoneyParser;
import com.gestorgastos.gestor_gastos.service.ia.InterpretacionGastoRequest;
import com.gestorgastos.gestor_gastos.service.ia.InterpretacionGastoResponse;
import com.gestorgastos.gestor_gastos.service.ia.LocalRulesProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class B63AndB64FixesTest {

    @Test
    void moneyParserAceptaCeroDecimalesExactosYRechazaFraccionesYNegativos() {
        assertEquals(new BigDecimal("60000"), MoneyParser.parseCopAcceptingZeroDecimals("60000").orElseThrow());
        assertEquals(new BigDecimal("60000"), MoneyParser.parseCopAcceptingZeroDecimals("60000.00").orElseThrow());
        assertEquals(new BigDecimal("45000"), MoneyParser.parseCopAcceptingZeroDecimals("45000.00").orElseThrow());
        assertEquals(new BigDecimal("60000"), MoneyParser.parseCopAcceptingZeroDecimals("60.000").orElseThrow());

        assertTrue(MoneyParser.parseCop("50.00").isEmpty(), "parseCop estricto mantiene rechazo de .00 manual");
        assertTrue(MoneyParser.parseCopAcceptingZeroDecimals("60000.50").isEmpty(), "No debe redondear ni aceptar decimales no nulos");
        assertTrue(MoneyParser.parseCopAcceptingZeroDecimals("-60000").isEmpty());
        assertTrue(MoneyParser.parseCopAcceptingZeroDecimals("60,000.00").isEmpty());
        assertTrue(MoneyParser.parseCopAcceptingZeroDecimals("abc").isEmpty());
    }

    @Test
    void localRulesExtraeConceptoLimpioSinResiduosPorPesos() {
        LocalRulesProvider provider = new LocalRulesProvider();
        Optional<InterpretacionGastoResponse> res = provider.interpretar(
                new InterpretacionGastoRequest(1L, "Compre alimentos por 45000 pesos", 3, List.of("A", "B", "C"), LocalDate.now())
        );
        assertTrue(res.isPresent());
        assertEquals("Alimentos", res.get().concepto());
        assertEquals(0, new BigDecimal("45000").compareTo(res.get().montoCop()));
    }
}
